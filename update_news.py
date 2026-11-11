import xml.etree.ElementTree as ET
import urllib.parse
import json
import re
from datetime import datetime
from email.utils import parsedate_to_datetime
import cloudscraper
from bs4 import BeautifulSoup
from camoufox.sync_api import Camoufox

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

def clean_image_url(image_url, base_feed_url=""):
    if not image_url:
        return ""
    image_url = image_url.strip().strip("'").strip('"')
    if image_url.startswith("data:") or ".gif" in image_url.lower() or "cropped-favicon" in image_url.lower():
        return ""
    if not image_url.startswith("http") and base_feed_url:
        base_url = base_feed_url.split("/feed")[0]
        if image_url.startswith("/"):
            image_url = image_url[1:]
        image_url = f"{base_url}/{image_url}"
    
    # Limpiamos sufijos de recorte pequeños si queremos o dobles extensiones .jpg.webp
    image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
    if ".avif" in image_url.lower():
        clean_url = image_url.replace("https://", "").replace("http://", "")
        image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"
    return image_url

def get_og_image(article_url):
    try:
        response = scraper.get(article_url, timeout=8)
        if response.status_code == 200:
            match = re.search(r'<meta[^>]+(?:property|name)=["\'](?:og:image|twitter:image)["\'][^>]+content=["\']([^"\']+)["\']', response.text, re.IGNORECASE)
            if not match:
                match = re.search(r'<meta[^>]+content=["\']([^"\']+)["\'][^>]+(?:property|name)=["\'](?:og:image|twitter:image)["\']', response.text, re.IGNORECASE)
            if match:
                return match.group(1).strip()
    except Exception:
        pass
    return ""

def normalize_url(u):
    if not u:
        return ""
    return u.split("?")[0].rstrip("/").replace("http://", "https://").replace("www.unionrayo.es", "unionrayo.es")

def scrape_union_rayo_images_with_camoufox(target_links):
    """Abre Unión Rayo con Camoufox (invisible ante Bot Verification) y extrae las imágenes reales de JNews y og:image"""
    image_map = {}
    print(" -> Iniciando Camoufox (navegador anti-Cloudflare) para extraer fotos de Unión Rayo...")
    try:
        with Camoufox(headless="virtual", humanize=True) as browser:
            page = browser.new_page()
            page.goto("https://unionrayo.es/", wait_until="domcontentloaded", timeout=35000)
            page.wait_for_timeout(5000)
            
            # Si Cloudflare muestra la casilla de verificación, esperamos o hacemos clic humano
            if "Bot Verification" in page.title() or "Just a moment" in page.title() or "Un momento" in page.title():
                print(" -> Resolviendo espera de Cloudflare en Camoufox...")
                page.wait_for_timeout(6000)

            print(f" -> Portada cargada en Camoufox. Título: '{page.title()}'")

            # 1. Extraemos todas las imágenes de la portada (tanto el slider background-image como las tarjetas jeg_post)
            portada_data = page.evaluate("""() => {
                const map = {};
                
                // A) Carrusel principal (div.jeg_slide_item con background-image)
                document.querySelectorAll('.jeg_slide_item').forEach(slide => {
                    const style = slide.getAttribute('style') || '';
                    const bgMatch = style.match(/url\\(['"]?([^'"\\)]+)['"]?\\)/i);
                    const linkEl = slide.querySelector('a[href*="unionrayo.es"]');
                    if (bgMatch && linkEl) {
                        map[linkEl.href] = bgMatch[1];
                    }
                });

                // B) Bloques de noticias JNews (.jeg_post, article)
                document.querySelectorAll('.jeg_post, article').forEach(post => {
                    const linkEl = post.querySelector('a[href*="unionrayo.es"]');
                    if (!linkEl) return;
                    let imgUrl = '';
                    
                    const imgEl = post.querySelector('img');
                    if (imgEl) {
                        imgUrl = imgEl.getAttribute('data-src') || imgEl.getAttribute('data-lazy-src') || imgEl.currentSrc || imgEl.src || '';
                    }
                    if (!imgUrl || imgUrl.startsWith('data:')) {
                        const styleEl = post.querySelector('[style*="background-image"]');
                        if (styleEl) {
                            const m = styleEl.getAttribute('style').match(/url\\(['"]?([^'"\\)]+)['"]?\\)/i);
                            if (m) imgUrl = m[1];
                        }
                    }
                    if (imgUrl && !imgUrl.startsWith('data:')) {
                        map[linkEl.href] = imgUrl;
                    }
                });

                return map;
            }""")

            for k, v in portada_data.items():
                image_map[normalize_url(k)] = v

            print(f" -> Fotos cazadas directamente de la portada JNews: {len(image_map)}")

            # 2. Para cualquier noticia de las 10 que no estuviera aún en el mapa, leemos su og:image desde dentro del navegador
            for link in target_links:
                norm_link = normalize_url(link)
                if norm_link not in image_map or not image_map[norm_link]:
                    try:
                        page.goto(link, wait_until="domcontentloaded", timeout=15000)
                        page.wait_for_timeout(1500)
                        og_img = page.evaluate("""() => {
                            const meta = document.querySelector('meta[property="og:image"], meta[property="og:image:secure_url"], meta[name="twitter:image"]');
                            if (meta && meta.content) return meta.content;
                            const firstWpImg = document.querySelector('img[src*="wp-content/uploads"], [style*="wp-content/uploads"]');
                            if (firstWpImg) return firstWpImg.src || '';
                            return '';
                        }""")
                        if og_img and "cropped-favicon" not in og_img:
                            image_map[norm_link] = og_img
                    except Exception as e:
                        print(f"   [Aviso abriendo artículo]: {e}")

    except Exception as e:
        print(f" -> Error en Camoufox: {e}")

    return image_map

def fetch_union_rayo_browser():
    print("=== EXTRAYENDO UNIÓN RAYO (RSS2JSON + CAMOUFOX JNEWS) ===")
    news_items = []
    try:
        rss2json_url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Ffeed%2F"
        res = scraper.get(rss2json_url, timeout=15)
        
        if res.status_code == 200:
            data = res.json()
            if data.get("status") == "ok":
                items = data.get("items", [])
                links_to_find = [item.get("link", "") for item in items if item.get("link")]
                
                # Obtenemos el mapa de fotos reales usando Camoufox sobre los elementos jeg_slide_item / jeg_post y og:image
                camoufox_images = scrape_union_rayo_images_with_camoufox(links_to_find)
                
                for item in items:
                    title = BeautifulSoup(item.get("title", ""), "html.parser").text.strip()
                    link = item.get("link", "")
                    norm_link = normalize_url(link)
                    
                    raw_date = item.get("pubDate", "")
                    pub_date = raw_date
                    if raw_date:
                        try:
                            dt = datetime.strptime(raw_date, "%Y-%m-%d %H:%M:%S")
                            pub_date = dt.strftime("%a, %d %b %Y %H:%M:%S +0000")
                        except Exception:
                            pass

                    raw_desc = item.get("description", "")
                    description = BeautifulSoup(raw_desc, "html.parser").text
                    description = description.replace("Leer más", "").replace("[...]", "").replace("[…]", "...").strip()
                    content_html = item.get("content", "").replace('loading="lazy"', '').replace("loading='lazy'", "")

                    # 1. Imagen desde nuestro mapa de Camoufox (portada JNews o meta og:image)
                    image_url = camoufox_images.get(norm_link, "")

                    # 2. Respaldo por si viniera en el propio cuerpo HTML del RSS
                    if not image_url and content_html:
                        img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                        if img_match and "wp-content/uploads" in img_match.group(1):
                            image_url = img_match.group(1)

                    image_url = clean_image_url(image_url, "https://unionrayo.es")
                    print(f"   * [{title[:38]}...] -> {'OK: ' + image_url.split('/')[-1] if image_url else 'SIN FOTO'}")
                    
                    categories = item.get("categories")
                    if not isinstance(categories, list) or not categories:
                        categories = ["Unión Rayo"]

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
                
                con_foto = sum(1 for n in news_items if n["image"])
                print(f" -> ¡COMPLETADO! {len(news_items)} noticias de Unión Rayo guardadas ({con_foto} con foto real).")
    except Exception as e:
        print(f" -> Error crítico en fetch_union_rayo_browser: {e}")

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

                image_url = clean_image_url(image_url, feed_info["url"])
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
        
    all_news.extend(fetch_union_rayo_browser())
        
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
