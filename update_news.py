import xml.etree.ElementTree as ET
import json
import re
from datetime import datetime
from email.utils import parsedate_to_datetime
import cloudscraper

FEEDS = [
    {"url": "https://www.rayovallecano.es/rss", "fuente": "Oficial"},
    {"url": "https://unionrayo.es/feed/", "fuente": "Unión Rayo"},
    {"url": "https://bukaneros.org/feed/", "fuente": "Bukaneros"},
    {"url": "https://matagigantes.net/feed/", "fuente": "Matagigantes"}
]

# 1. Creamos el "navegador fantasma" para esquivar a Cloudflare
scraper = cloudscraper.create_scraper(browser={
    'browser': 'chrome',
    'platform': 'windows',
    'mobile': False
})

def get_og_image(article_url):
    """Visita la web real con el scraper para evitar bloqueos del Captcha"""
    try:
        response = scraper.get(article_url, timeout=10)
        if response.status_code == 200:
            html = response.text
            match = re.search(r'<meta\s+(?:property|name)=["\']og:image["\']\s+content=["\']([^"\']+)["\']', html, re.IGNORECASE)
            if match:
                return match.group(1)
    except:
        pass
    return ""

def fetch_and_parse_feed(feed_info):
    print(f"Obteniendo RSS de {feed_info['fuente']}...")
    news_items = []
    
    try:
        # Usamos cloudscraper en lugar de urllib
        response = scraper.get(feed_info["url"], timeout=15)
        response.raise_for_status() # Lanza error si la web devuelve 404 o 500
        xml_data = response.content
    except Exception as e:
        print(f"Error al descargar el RSS de {feed_info['fuente']}: {e}")
        return news_items

    try:
        # --- FIX MATAGIGANTES: Limpiar basura HTML antes del XML ---
        xml_text = xml_data.decode('utf-8', errors='ignore')
        
        # Buscamos dónde empieza realmente el XML o el RSS
        xml_start = xml_text.find('<?xml')
        if xml_start == -1:
            xml_start = xml_text.find('<rss')
            
        if xml_start != -1:
            xml_text = xml_text[xml_start:] # Recortamos todo lo anterior
            
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
                
                # --- FIX BUKANEROS: LIMPIEZA DE ETIQUETAS HTML EN EL RESUMEN ---
                description = ""
                desc_tag = item.find("description")
                if desc_tag is not None and desc_tag.text:
                    raw_desc = desc_tag.text.strip()
                    # Borramos todo lo que esté entre < > para dejar solo el texto legible
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
                
                # Buscamos en el HTML sin limpiar por si estuviera escondida ahí (Bukaneros)
                if not image_url and desc_tag is not None and desc_tag.text:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', desc_tag.text, re.IGNORECASE)
                    if img_match:
                        image_url = img_match.group(1)

                if not image_url and link:
                    image_url = get_og_image(link)

                # --- FIX AVIF Y LIMPIEZA DE RUTAS ---
                if image_url:
                    if image_url.startswith("data:") or ".gif" in image_url.lower():
                        image_url = ""
                    else:
                        if not image_url.startswith("http"):
                            base_url = feed_info["url"].split("/feed")[0]
                            if image_url.startswith("/"):
                                image_url = image_url[1:]
                            image_url = f"{base_url}/{image_url}"
                        
                        # Truco para Unión Rayo: quitar extensiones tipo .avif o .webp para forzar el JPG
                        image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
                        
                        # Si de todos modos es un AVIF, usamos el proxy para garantizar compatibilidad
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
