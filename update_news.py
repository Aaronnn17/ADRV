import xml.etree.ElementTree as ET
import urllib.parse
import json
import re
from datetime import datetime
from email.utils import parsedate_to_datetime
import cloudscraper
from bs4 import BeautifulSoup

FEEDS = [
    {"url": "https://www.rayovallecano.es/rss", "fuente": "Oficial"},
    {"url": "https://bukaneros.org/feed/", "fuente": "Bukaneros"},
    {"url": "https://matagigantes.net/feed/", "fuente": "Matagigantes"}
]

scraper = cloudscraper.create_scraper(browser={'browser': 'chrome', 'platform': 'windows', 'mobile': False})

def clean_image_url(image_url, base_feed_url=""):
    if not image_url: return ""
    image_url = image_url.strip().strip("'").strip('"')
    if image_url.startswith("data:") or ".gif" in image_url.lower() or "cropped-favicon" in image_url.lower() or "logo" in image_url.lower():
        return ""
    if not image_url.startswith("http") and base_feed_url:
        base_url = base_feed_url.split("/feed")[0]
        if image_url.startswith("/"):
            image_url = image_url[1:]
        image_url = f"{base_url}/{image_url}"
    
    if "wp-content/uploads" in image_url:
        image_url = image_url.split("?")[0]
        # Limpiamos el recorte para coger la imagen en resolución original
        image_url = re.sub(r'-\d+x\d+\.(jpg|jpeg|png|webp)$', r'.\1', image_url, flags=re.IGNORECASE)

    image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
    if ".avif" in image_url.lower():
        clean_url = image_url.replace("https://", "").replace("http://", "")
        image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"
    return image_url

def fetch_union_rayo_wp_api():
    print("=== EXTRAYENDO UNIÓN RAYO (API WP - 1 SOLA LLAMADA) ===")
    news_items = []
    try:
        # RSS vía rss2json
        rss2json_url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Ffeed%2F"
        res = scraper.get(rss2json_url, timeout=15)
        
        if res.status_code != 200 or res.json().get("status") != "ok":
            print(" -> Fallo al obtener el RSS de Unión Rayo vía rss2json.")
            return news_items
        items = res.json().get("items", [])

        # UNA sola llamada a la API para obtener featured media de todos los posts
        featured_map = {}  # {post_id_str: image_url, slug: image_url}
        try:
            base_api_url = "https://unionrayo.es/wp-json/wp/v2/posts?_embed&per_page=15"
            # Proxy limpio para evitar el bloqueo 403 de Cloudflare a la IP de GitHub
            proxy_url = f"https://api.allorigins.win/raw?url={urllib.parse.quote(base_api_url)}"
            
            api_res = scraper.get(proxy_url, timeout=15)
            if api_res.status_code == 200:
                for post in api_res.json():
                    pid = str(post.get("id", ""))
                    slug = post.get("slug", "")
                    embedded = post.get("_embedded", {})
                    media = embedded.get("wp:featuredmedia", [])
                    if media and isinstance(media, list):
                        img_url = media[0].get("source_url", "")
                        if img_url:
                            if pid: featured_map[pid] = img_url
                            if slug: featured_map[slug] = img_url
            print(f"   API WP: {len(featured_map) // 2} featured images obtenidas en bloque.")
        except Exception as e:
            print(f"   [Aviso] API WP falló: {e}")

        for item in items:
            title = BeautifulSoup(item.get("title", ""), "html.parser").text.strip()
            link = item.get("link", "")
            guid = item.get("guid", link)
            slug_match = link.rstrip('/').split('/')[-1]
            
            # Extraer post_id
            post_id_match = re.search(r'\?p=(\d+)', guid)
            post_id = post_id_match.group(1) if post_id_match else None

            raw_date = item.get("pubDate", "")
            pub_date = raw_date
            if raw_date:
                try:
                    pub_date = datetime.strptime(raw_date, "%Y-%m-%d %H:%M:%S").strftime("%a, %d %b %Y %H:%M:%S +0000")
                except: pass

            raw_desc = item.get("description", "")
            description = BeautifulSoup(raw_desc, "html.parser").text.replace("Leer más", "").replace("[...]", "").replace("[…]", "...").strip()
            content_html = item.get("content", "").replace('loading="lazy"', '')
            
            image_url = ""

            # 1ª: featured media desde la API (por ID o por Slug)
            if post_id and post_id in featured_map:
                image_url = featured_map[post_id]
            elif slug_match and slug_match in featured_map:
                image_url = featured_map[slug_match]

            # 2ª: imagen embebida en content:encoded
            if not image_url and content_html:
                img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                if img_match and "wp-content/uploads" in img_match.group(1):
                    image_url = img_match.group(1)

            image_url = clean_image_url(image_url)
            print(f"   * [{title[:38]}...] -> {'OK: ' + image_url.split('/')[-1] if image_url else 'SIN FOTO'}")

            news_items.append({
                "id": guid, "title": title, "link": link,
                "published": pub_date, "summary": description,
                "content": content_html,
                "categories": item.get("categories", ["Unión Rayo"]),
                "image": image_url, "fuente": "Unión Rayo"
            })
        
        con_foto = sum(1 for n in news_items if n["image"])
        print(f" -> {len(news_items)} noticias de Unión Rayo procesadas ({con_foto} con foto).")
    except Exception as e:
        print(f" -> Error crítico en Unión Rayo: {e}")

    return news_items

def fetch_and_parse_feed(feed_info):
    print(f"Obteniendo RSS de {feed_info['fuente']}...")
    news_items = []
    try:
        response = scraper.get(feed_info["url"], timeout=10)
        xml_text = response.content.decode('utf-8', errors='ignore')
        xml_start = xml_text.find('<rss')
        if xml_start != -1:
            xml_text = xml_text[xml_start:]
        root = ET.fromstring(xml_text)
        namespaces = {'content': 'http://purl.org/rss/1.0/modules/content/', 'media': 'http://search.yahoo.com/mrss/'}
        
        for item in root.find("channel").findall("item"):
            title = item.find("title").text.strip() if item.find("title") is not None else ""
            link = item.find("link").text.strip() if item.find("link") is not None else ""
            pub_date = item.find("pubDate").text.strip() if item.find("pubDate") is not None else ""
            guid = item.find("guid").text.strip() if item.find("guid") is not None else link
            
            desc_tag = item.find("description")
            description = re.sub(r'<[^>]+>', '', desc_tag.text.strip()).replace("Leer más", "").replace("[&#8230;]", "...").strip() if desc_tag is not None and desc_tag.text else ""
            
            content_elem = item.find("content:encoded", namespaces)
            content_html = content_elem.text.strip().replace('loading="lazy"', '') if content_elem is not None and content_elem.text else ""
            
            image_url = ""
            if item.find("enclosure") is not None: image_url = item.find("enclosure").get("url", "")
            elif item.find("media:content", namespaces) is not None: image_url = item.find("media:content", namespaces).get("url", "")
            
            if not image_url and content_html:
                img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                if img_match: image_url = img_match.group(1)

            image_url = clean_image_url(image_url, feed_info["url"])
            cats = [cat.text.strip() for cat in item.findall("category") if cat.text]

            news_items.append({
                "id": guid, "title": title, "link": link, "published": pub_date,
                "summary": description, "content": content_html, "categories": cats,
                "image": image_url, "fuente": feed_info["fuente"]
            })
    except Exception as e:
         print(f"Error en {feed_info['fuente']}: {e}")
    return news_items

def update_news():
    all_news = []
    for feed in FEEDS:
        all_news.extend(fetch_and_parse_feed(feed))
        
    all_news.extend(fetch_union_rayo_wp_api())
        
    def sort_key(item):
        try: return parsedate_to_datetime(item["published"]).timestamp()
        except: return 0 

    all_news.sort(key=sort_key, reverse=True)
    with open("news.json", "w", encoding="utf-8") as f:
        json.dump({"updatedAt": datetime.now().isoformat(), "totalNews": len(all_news), "news": all_news}, f, ensure_ascii=False, indent=4)
    print(f"¡{len(all_news)} noticias extraídas en total!")

if __name__ == "__main__":
    update_news()
