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

def clean_image_url(image_url):
    if not image_url: return ""
    image_url = image_url.strip().strip("'").strip('"')
    if image_url.startswith("data:") or ".gif" in image_url.lower() or "cropped-favicon" in image_url.lower() or "logo" in image_url.lower():
        return ""
    if image_url.startswith("/"):
        image_url = f"https://unionrayo.es{image_url}"
        
    image_url = image_url.replace("unionrayo-es.translate.goog", "unionrayo.es")
    
    # Limpieza del recorte que vemos en tu captura (-350x250.jpeg -> .jpeg)
    if "wp-content/uploads" in image_url:
        image_url = image_url.split("?")[0]
        image_url = re.sub(r'-\d+x\d+\.(jpg|jpeg|png|webp)$', r'.\1', image_url, flags=re.IGNORECASE)

    image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
    if ".avif" in image_url.lower():
        clean_url = image_url.replace("https://", "").replace("http://", "")
        image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"
    return image_url

def get_images_from_homepage():
    """Hace scroll en la portada y caza las fotos usando los selectores exactos de tu captura"""
    image_map = {}
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
            context = browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                viewport={"width": 1440, "height": 900}
            )
            page = context.new_page()
            
            # Entramos por el traductor para que Cloudflare no pida el "Bot Verification"
            portada_url = "https://unionrayo-es.translate.goog/?_x_tr_sl=auto&_x_tr_tl=es&_x_tr_hl=es"
            page.goto(portada_url, wait_until="domcontentloaded", timeout=25000)
            page.wait_for_timeout(3000)
            
            page.evaluate("""async () => {
                await new Promise((resolve) => {
                    let totalHeight = 0;
                    let distance = 350;
                    let timer = setInterval(() => {
                        let scrollHeight = document.body.scrollHeight;
                        window.scrollBy(0, distance);
                        totalHeight += distance;
                        if (totalHeight >= scrollHeight || totalHeight > 6000) {
                            clearInterval(timer);
                            resolve();
                        }
                    }, 150);
                });
            }""")
            page.wait_for_timeout(2000)
            
            # Extraemos basándonos en tu captura: .jeg_post y .jeg_thumb
            cards_data = page.evaluate("""() => {
                const results = {};
                
                // 1. Carrusel de arriba (fondo CSS)
                document.querySelectorAll('.jeg_slide_item').forEach(slide => {
                    const a = slide.querySelector('a[href]');
                    const style = slide.getAttribute('style') || '';
                    const match = style.match(/url\\(['"]?([^'"\\)]+)['"]?\\)/i);
                    if (a && match) {
                        results[a.href] = match[1];
                    }
                });

                // 2. Noticias estándar (la de tu captura)
                document.querySelectorAll('article.jeg_post').forEach(article => {
                    const a = article.querySelector('.jeg_thumb a');
                    if (!a || !a.href) return;
                    
                    const img = a.querySelector('img');
                    if (img) {
                        // Priorizamos src de alta calidad, o el src base
                        let photo = img.getAttribute('data-src') || img.currentSrc || img.src || '';
                        
                        if (photo && !photo.startsWith('data:')) {
                            results[a.href] = photo;
                        }
                    }
                });
                
                return results;
            }""")
            
            for url_key, foto_url in cards_data.items():
                clean_key = url_key.split('?')[0].replace('unionrayo-es.translate.goog', 'unionrayo.es').rstrip('/')
                image_map[clean_key] = clean_image_url(foto_url)
                
            browser.close()
    except Exception as e:
        print(f" -> Aviso Playwright Portada: {e}")
        
    return image_map

def fetch_union_rayo():
    print("=== EXTRAYENDO UNIÓN RAYO (PORTADA PLAYWRIGHT REFINADA) ===")
    news_items = []
    try:
        # Obtenemos textos seguros con rss2json
        rss2json_url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Ffeed%2F"
        res = scraper.get(rss2json_url, timeout=15)
        
        if res.status_code == 200 and res.json().get("status") == "ok":
            items = res.json().get("items", [])
            
            print(" -> Cargando portada para cazar fotos con selectores exactos...")
            image_map = get_images_from_homepage()
            print(f" -> Fotos indexadas desde portada: {len(image_map)}")
            
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
                
                # Cruzamos la URL de la noticia con nuestro mapa de la portada
                image_url = image_map.get(clean_link, "")
                
                if not image_url and content_html:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                    if img_match and "wp-content/uploads" in img_match.group(1):
                        image_url = clean_image_url(img_match.group(1))
                        
                print(f"   * [{title[:38]}...] -> {'OK' if image_url else 'SIN FOTO'}")

                news_items.append({
                    "id": item.get("guid", link),
                    "title": title,
                    "link": link,
                    "published": pub_date,
                    "summary": description,
                    "content": content_html,
                    "categories": item.get("categories", ["Unión Rayo"]),
                    "image": image_url,
                    "fuente": "Unión Rayo"
                })
            
            print(f" -> ¡ÉXITO! {len(news_items)} noticias procesadas.")
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

            image_url = clean_image_url(image_url)
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
        
    all_news.extend(fetch_union_rayo())
        
    def sort_key(item):
        try: return parsedate_to_datetime(item["published"]).timestamp()
        except: return 0 

    all_news.sort(key=sort_key, reverse=True)
    with open("news.json", "w", encoding="utf-8") as f:
        json.dump({"updatedAt": datetime.now().isoformat(), "totalNews": len(all_news), "news": all_news}, f, ensure_ascii=False, indent=4)
    print(f"¡{len(all_news)} noticias extraídas en total!")

if __name__ == "__main__":
    update_news()
