import urllib.request
import xml.etree.ElementTree as ET
import json
import re # Librería para buscar texto
from datetime import datetime
from email.utils import parsedate_to_datetime

FEEDS = [
    {"url": "https://www.rayovallecano.es/rss", "fuente": "Oficial"},
    {"url": "https://unionrayo.es/feed/", "fuente": "Unión Rayo"},
    {"url": "https://bukaneros.org/feed/", "fuente": "Bukaneros"},
    {"url": "https://matagigantes.net/feed/", "fuente": "Matagigantes"}
]

# --- NUEVA FUNCIÓN SALVAVIDAS ---
def get_og_image(article_url):
    """Visita la web real de la noticia y extrae la miniatura oficial (og:image)"""
    try:
        req = urllib.request.Request(article_url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
        # Ponemos un timeout corto para no ralentizar el script si una web va lenta
        with urllib.request.urlopen(req, timeout=5) as response:
            html = response.read().decode('utf-8', errors='ignore')
            # Busca la etiqueta estándar de imagen de portada
            match = re.search(r'<meta\s+(?:property|name)=["\']og:image["\']\s+content=["\']([^"\']+)["\']', html, re.IGNORECASE)
            if match:
                return match.group(1)
    except Exception as e:
        pass
    return ""
# --------------------------------

def fetch_and_parse_feed(feed_info):
    print(f"Obteniendo RSS de {feed_info['fuente']}...")
    news_items = []
    
    try:
        req = urllib.request.Request(
            feed_info["url"], 
            headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
        )
        with urllib.request.urlopen(req, timeout=10) as response:
            xml_data = response.read()
    except Exception as e:
        print(f"Error al descargar el RSS de {feed_info['fuente']}: {e}")
        return news_items

    try:
        root = ET.fromstring(xml_data)
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
                    description = desc_tag.text.strip()

                content_html = ""
                content_elem = item.find("content:encoded", namespaces)
                if content_elem is not None and content_elem.text:
                    content_html = content_elem.text.strip()
                
                image_url = ""
                enclosure = item.find("enclosure")
                media_content = item.find("media:content", namespaces)
                media_thumbnail = item.find("media:thumbnail", namespaces)
                
                # A) Búsqueda en etiquetas estándar RSS
                if enclosure is not None:
                    image_url = enclosure.get("url", "")
                elif media_content is not None:
                    image_url = media_content.get("url", "")
                elif media_thumbnail is not None:
                    image_url = media_thumbnail.get("url", "")

                # B) Búsqueda en el código HTML del artículo RSS
                if not image_url and content_html:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                    if img_match:
                        image_url = img_match.group(1)
                
                if not image_url and description:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', description, re.IGNORECASE)
                    if img_match:
                        image_url = img_match.group(1)

                # C) EXTRACCIÓN EXTREMA: Si sigue vacía, visitamos el enlace real
                if not image_url and link:
                    image_url = get_og_image(link)

                # --- NUEVO BLOQUE: LIMPIEZA EXTREMA DE URLS ---
                if image_url:
                    # 1. Descartamos basura (píxeles, GIFs o imágenes rotas conocidas)
                    if image_url.startswith("data:") or ".gif" in image_url.lower() or image_url.endswith("/0-2.jpg"):
                        image_url = ""
                    else:
                        # 2. Si es una ruta relativa (empieza por / o no tiene http), forzamos la ruta completa
                        if not image_url.startswith("http"):
                            # Averiguamos el dominio base cortando la URL original de la fuente
                            base_url = feed_info["url"].split("/feed")[0]
                            
                            # Limpiamos la barra inicial para que no se duplique al juntarlo
                            if image_url.startswith("/"):
                                image_url = image_url[1:]
                                
                            image_url = f"{base_url}/{image_url}"

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

    # Ordenamos todas juntas por fecha (de más nueva a más antigua)
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
