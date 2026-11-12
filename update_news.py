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

# Scraper disfrazado de Googlebot para saltar el Bot Verification de Cloudflare sin usar Playwright
scraper = cloudscraper.create_scraper()
GOOGLEBOT_HEADERS = {
    "User-Agent": "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
}

def clean_image_url(image_url, base_feed_url=""):
    if not image_url:
        return ""
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
        image_url = re.sub(r'-\d+x\d+\.(jpg|jpeg|png|webp)$', r'.\1', image_url, flags=re.IGNORECASE)

    image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
    if ".avif" in image_url.lower():
        clean_url = image_url.replace("https://", "").replace("http://", "")
        image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"
    return image_url

def get_sitemap_images():
    """Descarga el Sitemap de Unión Rayo (permitido por Cloudflare) para mapear URLs con sus fotos"""
    image_map = {}
    sitemap_urls = [
        "https://unionrayo.es/post-sitemap.xml",
        "https://unionrayo.es/sitemap.xml",
        "https://unionrayo.es/sitemap-1.xml"
    ]
    
    for url in sitemap_urls:
        try:
            res = scraper.get(url, headers=GOOGLEBOT_HEADERS, timeout=8)
            if res.status_code == 200 and "<url>" in res.text:
                blocks = res.text.split("<url>")
                for block in blocks[1:]:
                    loc_match = re.search(r'<loc>([^<]+)</loc>', block)
                    img_match = re.search(r'<(?:image:loc|news:loc)[^>]*>([^<]+\.(?:jpg|jpeg|png|webp))<', block, re.IGNORECASE)
                    
                    if loc_match and img_match:
                        clean_loc = loc_match.group(1).split('?')[0].rstrip('/')
                        image_map[clean_loc] = clean_image_url(img_match.group(1).strip())
                if image_map:
                    print(f" -> Sitemap XML leído correctamente: {len(image_map)} fotos indexadas.")
                    break
        except Exception:
            continue
    return image_map

def fetch_union_rayo_fast():
    print("=== EXTRAYENDO UNIÓN RAYO (RSS2JSON + SITEMAP GOOGLEBOT) ===")
    news_items = []
    try:
        # Usamos el feed de la categoría Primer Equipo que sugeriste
        rss2json_url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Fnuestros-equipos%2Fprimer-equipo%2Ffeed%2F"
        res = scraper.get(rss2json_url, timeout=10)
        
        if res.status_code == 200:
            data = res.json()
            if data.get("status") == "ok":
                items = data.get("items", [])
                
                # Cargamos el mapa de fotos del Sitemap en 1 segundo
                sitemap_images = get_sitemap_images()

                for item in items:
                    title = BeautifulSoup(item.get("title", ""), "html.parser").text.strip()
                    link = item.get("link", "")
                    clean_link = link.split('?')[0].rstrip('/')
                    
                    raw_date = item.get("pubDate", "")
                    pub_date = raw_date
                    if raw_date:
                        try:
                            pub_date = datetime.strptime(raw_date, "%Y-%m-%d %H:%M:%S").strftime("%a, %d %b %Y %H:%M:%S +0000")
                        except: pass

                    raw_desc = item.get("description", "")
                    description = BeautifulSoup(raw_desc, "html.parser").text.replace("Leer más", "").replace("[...]", "").replace("[…]", "...").strip()
                    content_html = item.get("content", "").replace('loading="lazy"', '')

                    # 1. Buscamos la foto en el mapa del Sitemap
                    image_url = sitemap_images.get(clean_link, "")

                    # 2. Si no está en el Sitemap, entramos a la noticia disfrazados de Googlebot
                    if not image_url and link:
                        try:
                            art_res = scraper.get(link, headers=GOOGLEBOT_HEADERS, timeout=6)
                            if art_res.status_code == 200:
                                meta_match = re.search(r'<meta[^>]+property=["\']og:image["\'][^>]+content=["\']([^"\']+)["\']', art_res.text, re.IGNORECASE)
                                if meta_match:
                                    image_url = clean_image_url(meta_match.group(1))
                        except: pass

                    # 3. Respaldo final en el cuerpo HTML
                    if not image_url and content_html:
                        img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                        if img_match and "wp-content/uploads" in img_match.group(1):
                            image_url = clean_image_url(img_match.group(1))

                    print(f"   * [{title[:38]}...] -> {'OK: ' + image_url.split('/')[-1] if image_url else 'SIN FOTO'}")
                    
                    news_items.append({
                        "id": item.get("guid", link),
                        "title": title,
                        "link": link,
                        "published": pub_date,
                        "summary": description,
                        "content": content_html,
                        "categories": item.get("categories", ["Primer Equipo"]),
                        "image": image_url,
                        "fuente": "Unión Rayo"
                    })
                
                con_foto = sum(1 for n in news_items if n["image"])
                print(f" -> ¡COMPLETADO! {len(news_items)} noticias guardadas ({con_foto} con foto real).")
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
    all_news.extend(fetch_union_rayo_fast())
        
    def sort_key(item):
        try: return parsedate_to_datetime(item["published"]).timestamp()
        except: return 0 

    all_news.sort(key=sort_key, reverse=True)
    with open("news.json", "w", encoding="utf-8") as f:
        json.dump({"updatedAt": datetime.now().isoformat(), "totalNews": len(all_news), "news": all_news}, f, ensure_ascii=False, indent=4)
    print(f"¡{len(all_news)} noticias extraídas en total!")

if __name__ == "__main__":
    update_news()
    update_news()
