import xml.etree.ElementTree as ET
import json
import re
from datetime import datetime
from email.utils import parsedate_to_datetime
import cloudscraper
from bs4 import BeautifulSoup # Aprovechamos que ya lo instalaste en update.yml

FEEDS = [
    {"url": "https://www.rayovallecano.es/rss", "fuente": "Oficial"},
    {"url": "https://bukaneros.org/feed/", "fuente": "Bukaneros"},
    {"url": "https://matagigantes.net/feed/", "fuente": "Matagigantes"}
    # Unión Rayo lo sacamos de aquí para usar su API nativa
]

scraper = cloudscraper.create_scraper(browser={
    'browser': 'chrome',
    'platform': 'windows',
    'mobile': False
})

def get_og_image(article_url):
    try:
        response = scraper.get(article_url, timeout=10)
        if response.status_code == 200:
            match = re.search(r'<meta\s+(?:property|name)=["\']og:image["\']\s+content=["\']([^"\']+)["\']', response.text, re.IGNORECASE)
            if match:
                return match.group(1)
    except:
        pass
    return ""

def fetch_union_rayo_api():
    print("Obteniendo noticias de Unión Rayo vía pasarela rss2json...")
    news_items = []
    try:
        # Usamos rss2json como puente para saltar el bloqueo de IP de GitHub Actions
        url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Ffeed%2F"
        response = scraper.get(url, timeout=15)
        
        if response.status_code == 200:
            data = response.json()
            if data.get("status") == "ok":
                for item in data.get("items", []):
                    title = BeautifulSoup(item.get("title", ""), "html.parser").text.strip()
                    link = item.get("link", "")
                    
                    # Convertimos la fecha 'YYYY-MM-DD HH:MM:SS' al formato estándar RSS
                    raw_date = item.get("pubDate", "")
                    pub_date = raw_date
                    if raw_date:
                        try:
                            dt = datetime.strptime(raw_date, "%Y-%m-%d %H:%M:%S")
                            pub_date = dt.strftime("%a, %d %b %Y %H:%M:%S +0000")
                        except:
                            pass
                    
                    raw_desc = item.get("description", "")
                    description = BeautifulSoup(raw_desc, "html.parser").text
                    description = description.replace("Leer más", "").replace("[...]", "...").replace("[…]", "...").strip()
                    
                    content_html = item.get("content", "").replace('loading="lazy"', '').replace("loading='lazy'", "")
                    
                    # Extraemos la imagen del JSON o buscando dentro del HTML del artículo (sin visitar la web)
                    image_url = item.get("thumbnail", "") or item.get("enclosure", {}).get("link", "")
                    if not image_url and content_html:
                        img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                        if img_match:
                            image_url = img_match.group(1)
                    if not image_url and raw_desc:
                        img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', raw_desc, re.IGNORECASE)
                        if img_match:
                            image_url = img_match.group(1)
                            
                    if image_url:
                        image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
                        if ".avif" in image_url.lower():
                            clean_url = image_url.replace("https://", "").replace("http://", "")
                            image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"

                    categories = item.get("categories", ["Unión Rayo"])

                    news_items.append({
                        "id": item.get("guid", link),
                        "title": title,
                        "link": link,
                        "published": pub_date,
                        "summary": description,
                        "content": content_html,
                        "categories": categories,
                        "image": image_url,
                        "fuente": "Unión Rayo"
                    })
                print(f" -> {len(news_items)} noticias de Unión Rayo rescatadas con éxito.")
            else:
                print(f" -> Error en rss2json: {data.get('message')}")
        else:
            print(f" -> Fallo HTTP {response.status_code} al conectar con rss2json")
    except Exception as e:
        print(f"Error con la pasarela de Unión Rayo: {e}")
         
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
    
    # 1. Obtenemos las noticias de los RSS normales
    for feed in FEEDS:
        all_news.extend(fetch_and_parse_feed(feed))
        
    # 2. Obtenemos las de Unión Rayo directamente desde su API JSON
    all_news.extend(fetch_union_rayo_api())
        
    def sort_key(item):
        try:
            dt = parsedate_to_datetime(item["published"])
            return dt.timestamp()
        except:
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
