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
        # Limpiamos recortes para obtener la foto en alta resolución
        image_url = re.sub(r'-\d+x\d+\.(jpg|jpeg|png|webp)$', r'.\1', image_url, flags=re.IGNORECASE)

    image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
    if ".avif" in image_url.lower():
        clean_url = image_url.replace("https://", "").replace("http://", "")
        image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"
    return image_url

def fetch_union_rayo_wp_api():
    print("=== EXTRAYENDO UNIÓN RAYO (VÍA WP REST API) ===")
    news_items = []
    try:
        # 1. Obtenemos las noticias de forma segura con rss2json
        rss2json_url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Ffeed%2F"
        res = scraper.get(rss2json_url, timeout=15)
        
        if res.status_code == 200 and res.json().get("status") == "ok":
            items = res.json().get("items", [])
            
            for item in items:
                title = BeautifulSoup(item.get("title", ""), "html.parser").text.strip()
                link = item.get("link", "")
                guid = item.get("guid", link)
                
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

                # 2. LA MAGIA: Extraer ID del post desde el guid y llamar a la API REST
                post_id_match = re.search(r'\?p=(\d+)', guid)
                if post_id_match:
                    post_id = post_id_match.group(1)
                    try:
                        # Usamos ?_embed para que devuelva la URL de la foto en la misma petición
                        wp_api_url = f"https://unionrayo.es/wp-json/wp/v2/posts/{post_id}?_embed"
                        # Pasamos por un proxy CORS limpio para esquivar a Cloudflare
                        proxy_url = f"https://api.allorigins.win/raw?url={urllib.parse.quote(wp_api_url)}"
                        
                        api_res = scraper.get(proxy_url, timeout=10)
                        if api_res.status_code == 200:
                            post_data = api_res.json()
                            # Extraemos la imagen destacada del JSON anidado
                            embedded = post_data.get("_embedded", {})
                            wp_featuredmedia = embedded.get("wp:featuredmedia", [])
                            if wp_featuredmedia and isinstance(wp_featuredmedia, list):
                                image_url = wp_featuredmedia[0].get("source_url", "")
                    except Exception as e:
                        print(f"   [Aviso API en {title[:20]}...]: {e}")

                # 3. Respaldo por si falla la API (busca en el cuerpo HTML)
                if not image_url and content_html:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                    if img_match and "wp-content/uploads" in img_match.group(1):
                        image_url = img_match.group(1)

                image_url = clean_image_url(image_url)
                print(f"   * [{title[:38]}...] -> {'OK: ' + image_url.split('/')[-1] if image_url else 'SIN FOTO'}")

                news_items.append({
                    "id": guid,
                    "title": title,
                    "link": link,
                    "published": pub_date,
                    "summary": description,
                    "content": content_html,
                    "categories": item.get("categories", ["Unión Rayo"]),
                    "image": image_url,
                    "fuente": "Unión Rayo"
                })
            
            con_foto = sum(1 for n in news_items if n["image"])
            print(f" -> ¡ÉXITO! {len(news_items)} noticias procesadas ({con_foto} con foto).")
    except Exception as e:
        print(f" -> Error crítico: {e}")

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
