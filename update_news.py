import xml.etree.ElementTree as ET
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

scraper = cloudscraper.create_scraper(browser={
    'browser': 'chrome',
    'platform': 'windows',
    'mobile': False
})

def clean_image_url(image_url, base_feed_url=""):
    if not image_url:
        return ""
    if image_url.startswith("data:") or ".gif" in image_url.lower() or "cropped-favicon" in image_url.lower():
        return ""
    if not image_url.startswith("http") and base_feed_url:
        base_url = base_feed_url.split("/feed")[0]
        if image_url.startswith("/"):
            image_url = image_url[1:]
        image_url = f"{base_url}/{image_url}"
    
    # Quitar doble extensión tipo .jpg.webp o .jpg.avif de Unión Rayo
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
    print("=== EXTRAYENDO UNIÓN RAYO (JETPACK API + CHROMIUM) ===")
    news_items = []

    # 1. INTENTO PRINCIPAL: API Oficial de WordPress.com usando el Site ID 234065985 de Unión Rayo
    try:
        jetpack_url = "https://public-api.wordpress.com/rest/v1.1/sites/234065985/posts/?number=15"
        res = scraper.get(jetpack_url, timeout=15)
        print(f" -> Estado API Jetpack WordPress.com: HTTP {res.status_code}")
        
        if res.status_code == 200:
            data = res.json()
            posts = data.get("posts", [])
            for post in posts:
                title = BeautifulSoup(post.get("title", ""), "html.parser").text.strip()
                link = post.get("URL", "")
                post_id = post.get("ID", "")
                
                raw_date = post.get("date", "")
                pub_date = raw_date
                if raw_date:
                    try:
                        dt = datetime.fromisoformat(raw_date)
                        pub_date = dt.strftime("%a, %d %b %Y %H:%M:%S +0000")
                    except Exception:
                        pass

                raw_desc = post.get("excerpt", "")
                description = BeautifulSoup(raw_desc, "html.parser").text
                description = description.replace("Leer más", "").replace("[...]", "").replace("[…]", "...").strip()
                
                content_html = post.get("content", "").replace('loading="lazy"', '').replace("loading='lazy'", "")

                # Foto directa desde los servidores de WordPress/Jetpack
                image_url = post.get("featured_image", "")
                if not image_url and isinstance(post.get("post_thumbnail"), dict):
                    image_url = post["post_thumbnail"].get("URL", "")
                if not image_url and isinstance(post.get("attachments"), dict):
                    for att in post["attachments"].values():
                        if isinstance(att, dict) and att.get("URL"):
                            image_url = att["URL"]
                            break
                if not image_url and content_html:
                    img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                    if img_match:
                        image_url = img_match.group(1)

                image_url = clean_image_url(image_url, "https://unionrayo.es")

                cats_dict = post.get("categories", {})
                categories = list(cats_dict.keys()) if isinstance(cats_dict, dict) and cats_dict else ["Unión Rayo"]

                news_items.append({
                    "id": f"https://unionrayo.es/?p={post_id}" if post_id else link,
                    "title": title,
                    "link": link,
                    "published": pub_date,
                    "summary": description,
                    "content": content_html,
                    "categories": categories[:4],
                    "image": image_url,
                    "fuente": "Unión Rayo"
                })
            if news_items:
                con_foto = sum(1 for n in news_items if n["image"])
                print(f" -> ¡ÉXITO vía Jetpack! {len(news_items)} noticias de Unión Rayo ({con_foto} con foto).")
                return news_items
    except Exception as e:
        print(f" -> Aviso en API Jetpack: {e}")

    # 2. PLAN B: Rss2Json + Rescate de fotos con Chromium camuflado
    try:
        print(" -> Probando pasarela rss2json + navegador Chromium camuflado...")
        rss2json_url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Funionrayo.es%2Ffeed%2F"
        res = scraper.get(rss2json_url, timeout=15)
        if res.status_code == 200 and res.json().get("status") == "ok":
            items = res.json().get("items", [])
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
                description = BeautifulSoup(raw_desc, "html.parser").text.replace("Leer más", "").replace("[…]", "...").strip()
                content_html = item.get("content", "").replace('loading="lazy"', '')
                image_url = item.get("thumbnail", "") or item.get("enclosure", {}).get("link", "")

                news_items.append({
                    "id": item.get("guid", link),
                    "title": title,
                    "link": link,
                    "published": pub_date,
                    "summary": description,
                    "content": content_html,
                    "categories": item.get("categories", ["Unión Rayo"]),
                    "image": clean_image_url(image_url, "https://unionrayo.es"),
                    "fuente": "Unión Rayo"
                })
    except Exception as e:
        print(f" -> Aviso en rss2json: {e}")

    # Si tenemos noticias pero faltan fotos (o si rss2json falló), abrimos Chromium ocultando HeadlessChrome
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(
                headless=True,
                args=[
                    "--disable-blink-features=AutomationControlled",
                    "--no-sandbox",
                    "--disable-dev-shm-usage"
                ]
            )
            context = browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                extra_http_headers={
                    "sec-ch-ua": '"Chromium";v="124", "Google Chrome";v="124", "Not-A.Brand";v="99"',
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": '"Windows"',
                    "Accept-Language": "es-ES,es;q=0.9"
                },
                viewport={"width": 1366, "height": 768}
            )
            page = context.new_page()
            page.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
            
            page.goto("https://unionrayo.es/", wait_until="domcontentloaded", timeout=25000)
            page.wait_for_timeout(3500)
            print(f" -> Título cargado en Chromium: {page.title()}")

            # Mapa de imágenes de la portada de Unión Rayo
            portada_imgs = page.evaluate("""() => {
                const map = {};
                document.querySelectorAll('article, .td-module-container, .post, .jeg_post').forEach(el => {
                    const a = el.querySelector('a[href*="unionrayo.es"]');
                    const img = el.querySelector('img');
                    if (a && img) {
                        const src = img.getAttribute('data-src') || img.getAttribute('data-lazy-src') || img.currentSrc || img.src || '';
                        if (src && !src.startsWith('data:')) map[a.href] = src;
                    }
                });
                return map;
            }""")

            for item in news_items:
                if not item["image"] and item["link"] in portada_imgs:
                    item["image"] = clean_image_url(portada_imgs[item["link"]], "https://unionrayo.es")

            browser.close()
    except Exception as e:
        print(f" -> Aviso en Chromium: {e}")

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
