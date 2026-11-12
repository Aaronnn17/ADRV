import xml.etree.ElementTree as ET
import urllib.parse
import json
import re
from datetime import datetime
from email.utils import parsedate_to_datetime
import cloudscraper
from bs4 import BeautifulSoup
from playwright.sync_api import sync_playwright

FEEDS = [
    {"url": "https://www.rayovallecano.es/rss", "fuente": "Oficial"},
    {"url": "https://bukaneros.org/feed/", "fuente": "Bukaneros"},
    {"url": "https://matagigantes.net/feed/", "fuente": "Matagigantes"}
]

scraper = cloudscraper.create_scraper(browser={'browser': 'chrome', 'platform': 'windows', 'mobile': False})

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
    
    # Limpieza de proxy y recorte de WordPress (-350x250.jpg -> .jpg) para foto original
    image_url = image_url.replace("unionrayo-es.translate.goog", "unionrayo.es")
    if "wp-content/uploads" in image_url:
        image_url = image_url.split("?")[0]
        image_url = re.sub(r'-\d+x\d+\.(jpg|jpeg|png|webp)$', r'.\1', image_url, flags=re.IGNORECASE)

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

def fetch_union_rayo_browser():
    print("=== EXTRAYENDO UNIÓN RAYO (ENTRADA DIRECTA CON PLAYWRIGHT) ===")
    news_items = []
    try:
        rss2json_url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Ffeed%2F"
        res = scraper.get(rss2json_url, timeout=15)
        
        if res.status_code == 200:
            data = res.json()
            if data.get("status") == "ok":
                items = data.get("items", [])
                
                # Lanzamos el navegador real como hicimos en la plantilla
                with sync_playwright() as p:
                    browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-blink-features=AutomationControlled"])
                    context = browser.new_context(
                        user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    )
                    page = context.new_page()

                    for item in items:
                        title = BeautifulSoup(item.get("title", ""), "html.parser").text.strip()
                        link = item.get("link", "")
                        
                        raw_date = item.get("pubDate", "")
                        pub_date = raw_date
                        if raw_date:
                            try:
                                dt = datetime.strptime(raw_date, "%Y-%m-%d %H:%M:%S")
                                pub_date = dt.strftime("%a, %d %b %Y %H:%M:%S +0000")
                            except Exception:
                                pass

                        raw_desc = item.get("description", "")
                        description = BeautifulSoup(raw_desc, "html.parser").text.replace("Leer más", "").replace("[...]", "").replace("[…]", "...").strip()
                        content_html = item.get("content", "").replace('loading="lazy"', '').replace("loading='lazy'", "")

                        image_url = ""

                        # 1. EL NAVEGADOR ENTRA FÍSICAMENTE EN LA NOTICIA
                        if link:
                            try:
                                # Entrada directa al artículo
                                page.goto(link, wait_until="domcontentloaded", timeout=15000)
                                page.wait_for_timeout(1000)
                                
                                # Si Cloudflare frena a Playwright, usamos la puerta trasera de Google para esta noticia
                                if "Bot Verification" in page.title() or "Just a moment" in page.title():
                                    parsed = urllib.parse.urlparse(link)
                                    bridge_url = f"https://unionrayo-es.translate.goog{parsed.path}?_x_tr_sl=auto&_x_tr_tl=es&_x_tr_hl=es"
                                    page.goto(bridge_url, wait_until="domcontentloaded", timeout=15000)
                                    page.wait_for_timeout(1000)
                                
                                # Leemos la foto directamente de las etiquetas de la página cargada
                                img_extracted = page.evaluate("""() => {
                                    const meta = document.querySelector('meta[property="og:image"], meta[property="og:image:secure_url"]');
                                    if (meta && meta.content && meta.content.includes('wp-content/uploads')) return meta.content;
                                    
                                    const feat = document.querySelector('.jeg_featured img, .wp-post-image, img[src*="wp-content/uploads"]');
                                    if (feat) return feat.getAttribute('data-src') || feat.currentSrc || feat.src;
                                    
                                    return '';
                                }""")
                                
                                image_url = clean_image_url(img_extracted, "https://unionrayo.es")
                            except Exception as e:
                                print(f"   [Fallo al entrar en {title[:20]}...]: {e}")

                        # 2. Respaldo (por si acaso viniera incrustada en el código fuente RSS)
                        if not image_url and content_html:
                            img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                            if img_match and "wp-content/uploads" in img_match.group(1):
                                image_url = clean_image_url(img_match.group(1), "https://unionrayo.es")

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
                    
                    browser.close()
                
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
