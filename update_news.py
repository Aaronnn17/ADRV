import xml.etree.ElementTree as ET
import json
import re
import urllib.parse
from datetime import datetime
from email.utils import parsedate_to_datetime
import cloudscraper
from bs4 import BeautifulSoup

FEEDS = [
    {"url": "https://www.rayovallecano.es/rss", "fuente": "Oficial"},
    {"url": "https://bukaneros.org/feed/", "fuente": "Bukaneros"},
    {"url": "https://matagigantes.net/feed/", "fuente": "Matagigantes"}
]

scraper = cloudscraper.create_scraper(browser={
    'browser': 'chrome',
    'platform': 'windows',
    'mobile': False
})

def get_og_image(article_url):
    """Extrae la imagen de portada usando la API de Microlink (esquiva Cloudflare en 1 paso)"""
    if not article_url:
        return ""
    
    # Intento 1: API de Microlink (especializada en extraer metadatos y og:image sin bloqueos)
    try:
        encoded = urllib.parse.quote(article_url, safe='')
        micro_url = f"https://api.microlink.io/?url={encoded}"
        res = scraper.get(micro_url, timeout=8)
        if res.status_code == 200:
            datos = res.json()
            img = datos.get("data", {}).get("image", {}).get("url", "")
            if img and img.startswith("http") and "cropped-favicon" not in img:
                return img
    except Exception:
        pass

    # Intento 2: Petición directa por si es de otra web sin Cloudflare (ej. Oficial / Bukaneros)
    try:
        response = scraper.get(article_url, timeout=6)
        if response.status_code == 200:
            match = re.search(r'<meta[^>]+(?:property|name)=["\'](?:og:image|twitter:image)["\'][^>]+content=["\']([^"\']+)["\']', response.text, re.IGNORECASE)
            if not match:
                match = re.search(r'<meta[^>]+content=["\']([^"\']+)["\'][^>]+(?:property|name)=["\'](?:og:image|twitter:image)["\']', response.text, re.IGNORECASE)
            if match:
                return match.group(1).strip()
    except Exception:
        pass

    return ""

def fetch_union_rayo_api():
    print("=== INICIANDO EXTRACCIÓN DE UNIÓN RAYO ===")
    news_items = []
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept": "application/json, application/xml, text/xml, */*"
    }

    # --- INTENTO 1: API WordPress con fotos incluidas (1 sola petición vía túneles) ---
    wp_api_url = "https://unionrayo.es/wp-json/wp/v2/posts?per_page=12&_embed"
    encoded_wp = urllib.parse.quote(wp_api_url, safe='')
    
    tuneles_json = [
        ("CodeTabs WP-JSON", f"https://api.codetabs.com/v1/proxy?quest={encoded_wp}"),
        ("AllOrigins WP-JSON", f"https://api.allorigins.win/raw?url={encoded_wp}"),
        ("Directo WP-JSON", wp_api_url)
    ]

    for nombre, url in tuneles_json:
        try:
            print(f"Probando {nombre}...")
            res = scraper.get(url, headers=headers, timeout=12)
            if res.status_code == 200 and res.text.strip().startswith("["):
                posts = res.json()
                for post in posts:
                    title = BeautifulSoup(post.get("title", {}).get("rendered", ""), "html.parser").text.strip()
                    link = post.get("link", "")
                    
                    raw_date = post.get("date_gmt", "")
                    pub_date = raw_date
                    if raw_date:
                        try:
                            dt = datetime.fromisoformat(raw_date)
                            pub_date = dt.strftime("%a, %d %b %Y %H:%M:%S +0000")
                        except Exception:
                            pass

                    raw_desc = post.get("excerpt", {}).get("rendered", "")
                    description = BeautifulSoup(raw_desc, "html.parser").text.replace("Leer más", "").replace("[...]", "").replace("[…]", "...").strip()
                    content_html = post.get("content", {}).get("rendered", "").replace('loading="lazy"', '').replace("loading='lazy'", "")

                    # Foto directa desde el JSON de WordPress
                    image_url = post.get("jetpack_featured_media_url", "")
                    if not image_url and "_embedded" in post and "wp:featuredmedia" in post["_embedded"]:
                        media = post["_embedded"]["wp:featuredmedia"]
                        if isinstance(media, list) and len(media) > 0:
                            image_url = media[0].get("source_url", "")

                    if image_url:
                        image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
                        if ".avif" in image_url.lower():
                            clean_url = image_url.replace("https://", "").replace("http://", "")
                            image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"

                    news_items.append({
                        "id": f"https://unionrayo.es/?p={post.get('id', '')}" if post.get("id") else link,
                        "title": title,
                        "link": link,
                        "published": pub_date,
                        "summary": description,
                        "content": content_html,
                        "categories": ["Unión Rayo"],
                        "image": image_url,
                        "fuente": "Unión Rayo"
                    })
                if news_items:
                    print(f" -> ¡ÉXITO con {nombre}! {len(news_items)} noticias con foto extraídas.")
                    return news_items
        except Exception as e:
            print(f" -> Falló {nombre}: {e}")

    # --- INTENTO 2: Pasarelas RSS XML (Si fallan los JSON de arriba) ---
    rss_url = "https://unionrayo.es/feed/"
    encoded_rss = urllib.parse.quote(rss_url, safe='')
    
    tuneles_rss = [
        ("CodeTabs RSS", f"https://api.codetabs.com/v1/proxy?quest={encoded_rss}"),
        ("AllOrigins RSS", f"https://api.allorigins.win/raw?url={encoded_rss}"),
        ("CorsProxy RSS", f"https://corsproxy.io/?{encoded_rss}")
    ]

    for nombre, url in tuneles_rss:
        try:
            print(f"Probando {nombre}...")
            res = scraper.get(url, headers=headers, timeout=12)
            if res.status_code == 200 and ("<rss" in res.text or "<?xml" in res.text):
                xml_text = res.text
                xml_start = xml_text.find('<?xml')
                if xml_start == -1:
                    xml_start = xml_text.find('<rss')
                if xml_start != -1:
                    xml_text = xml_text[xml_start:]

                root = ET.fromstring(xml_text)
                channel = root.find("channel")
                namespaces = {'content': 'http://purl.org/rss/1.0/modules/content/'}
                
                if channel is not None:
                    for item in channel.findall("item"):
                        title = item.find("title").text.strip() if item.find("title") is not None else ""
                        link = item.find("link").text.strip() if item.find("link") is not None else ""
                        pub_date = item.find("pubDate").text.strip() if item.find("pubDate") is not None else ""
                        guid = item.find("guid").text.strip() if item.find("guid") is not None else link
                        
                        raw_desc = item.find("description").text.strip() if item.find("description") is not None and item.find("description").text else ""
                        description = re.sub(r'<[^>]+>', '', raw_desc).replace("Leer más", "").replace("[&#8230;]", "...").strip()
                        
                        content_elem = item.find("content:encoded", namespaces)
                        content_html = content_elem.text.strip() if content_elem is not None and content_elem.text else ""
                        
                        # Sin llamadas lentas para no colgar el script
                        image_url = ""
                        img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                        if img_match:
                            image_url = img_match.group(1)

                        categories = [cat.text.strip() for cat in item.findall("category") if cat.text]
                        news_items.append({
                            "id": guid,
                            "title": title,
                            "link": link,
                            "published": pub_date,
                            "summary": description,
                            "content": content_html,
                            "categories": categories,
                            "image": image_url,
                            "fuente": "Unión Rayo"
                        })
                    if news_items:
                        print(f" -> ¡ÉXITO con {nombre}! {len(news_items)} noticias extraídas.")
                        return news_items
        except Exception as e:
            print(f" -> Falló {nombre}: {e}")

    print(" -> Ninguna pasarela de Unión Rayo respondió a tiempo.")
    return news_items

def fetch_and_parse_feed(feed_info):
    print(f"Obteniendo RSS de {feed_info['fuente']}...")
    news_items = []
    
    try:
        response = scraper.get(feed_info["url"], timeout=15)
        response.raise_for_status()
        xml_data = response.content
    except Exception as e:
        print(f"Error al descargar el RSS de {feed_info['fuente']}: {e}")
        return news_items

    try:
        xml_text = xml_data.decode('utf-8', errors='ignore')
        xml_start = xml_text.find('<?xml')
        if xml_start == -1:
            xml_start = xml_text.find('<rss')
            
        if xml_start != -1:
            xml_text = xml_text[xml_start:]
            
        root = ET.fromstring(xml_text)
        channel = root.find("channel")
        namespaces = {
            'content': 'http://purl.org/rss/1.0/modules/content/',
            'media': 'http://search.yahoo.com/mrss/'
        }
        
        if channel is not None:
            for item in channel.findall("item"):
                title_elem = item.find("title")
                title = title_elem.text.strip() if title_elem is not None and title_elem.text else ""
                
                link_elem = item.find("link")
                link = link_elem.text.strip() if link_elem is not None and link_elem.text else ""
                
                pub_date_elem = item.find("pubDate")
                pub_date = pub_date_elem.text.strip() if pub_date_elem is not None and pub_date_elem.text else ""
                
                guid_elem = item.find("guid")
                guid = guid_elem.text.strip() if guid_elem is not None and guid_elem.text else link
                
                description = ""
                desc_tag = item.find("description")
                if desc_tag is not None and desc_tag.text:
                    raw_desc = desc_tag.text.strip()
                    description = re.sub(r'<[^>]+>', '', raw_desc).strip()
                    description = description.replace("Leer más", "").replace("[&#8230;]", "...").strip()

                content_html = ""
                content_elem = item.find("content:encoded", namespaces)
                if content_elem is not None and content_elem.text:
                    content_html = content_elem.text.strip()
                    content_html = content_html.replace('loading="lazy"', '').replace("loading='lazy'", "")
                
                image_url = ""
                enclosure = item.find("enclosure")
                media_content = item.find("media:content", namespaces)
                media_thumbnail = item.find("media:thumbnail", namespaces)
                
                if enclosure is not None:
                    image_url = enclosure.get("url", "")
                elif media_content is not None:
                    image_url = media_content.get("url", "")
                elif media_thumbnail is not None:
                    image_url = media_thumbnail.get("url", "")

                if not image_url and content_html:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                    if img_match:
                        image_url = img_match.group(1)
                
                if not image_url and desc_tag is not None and desc_tag.text:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', desc_tag.text, re.IGNORECASE)
                    if img_match:
                        image_url = img_match.group(1)

                if not image_url and link:
                    image_url = get_og_image(link)

                if image_url:
                    if image_url.startswith("data:") or ".gif" in image_url.lower():
                        image_url = ""
                    else:
                        if not image_url.startswith("http"):
                            base_url = feed_info["url"].split("/feed")[0]
                            if image_url.startswith("/"):
                                image_url = image_url[1:]
                            image_url = f"{base_url}/{image_url}"
                        
                        image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
                        if ".avif" in image_url.lower():
                            clean_url = image_url.replace("https://", "").replace("http://", "")
                            image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"

                categories = [cat.text.strip() for cat in item.findall("category") if cat.text]

                news_items.append({
                    "id": guid,
                    "title": title,
                    "link": link,
                    "published": pub_date,
                    "summary": description,
                    "content": content_html,
                    "categories": categories,
                    "image": image_url,
                    "fuente": feed_info["fuente"]
                })
    except Exception as e:
         print(f"Error parseando el XML de {feed_info['fuente']}: {e}")
         
    return news_items

def update_news():
    all_news = []
    
    for feed in FEEDS:
        all_news.extend(fetch_and_parse_feed(feed))
        
    all_news.extend(fetch_union_rayo_api())
        
    def sort_key(item):
        try:
            dt = parsedate_to_datetime(item["published"])
            return dt.timestamp()
        except Exception:
            return 0 

    all_news.sort(key=sort_key, reverse=True)

    data = {
        "updatedAt": datetime.now().isoformat(),
        "totalNews": len(all_news),
        "news": all_news
    }

    with open("news.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
    
    print(f"¡{len(all_news)} noticias extraídas en total y guardadas en news.json!")

if __name__ == "__main__":
    update_news()
