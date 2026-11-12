import xml.etree.ElementTree as ET
import json
import re
from datetime import datetime
from email.utils import parsedate_to_datetime
import cloudscraper

# Hemos añadido Unión Rayo directamente a la lista normal de feeds
FEEDS = [
    {"url": "https://www.rayovallecano.es/rss", "fuente": "Oficial"},
    {"url": "https://unionrayo.es/feed/", "fuente": "Unión Rayo"},
    {"url": "https://bukaneros.org/feed/", "fuente": "Bukaneros"},
    {"url": "https://matagigantes.net/feed/", "fuente": "Matagigantes"}
]

scraper = cloudscraper.create_scraper(browser={'browser': 'chrome', 'platform': 'windows', 'mobile': False})

def clean_image_url(image_url, base_feed_url="", strip_sizes=True):
    if not image_url:
        return ""
    image_url = image_url.strip().strip("'").strip('"')

    filename = image_url.split("?")[0].split("/")[-1].lower()
    if (image_url.startswith("data:") or ".gif" in image_url.lower()
            or "cropped-favicon" in filename or "logo" in filename):
        return ""

    if not image_url.startswith("http") and base_feed_url:
        base_url = base_feed_url.split("/feed")[0]
        if image_url.startswith("/"):
            image_url = image_url[1:]
        image_url = f"{base_url}/{image_url}"

    if "wp-content/uploads" in image_url:
        image_url = image_url.split("?")[0]
        if strip_sizes:
            image_url = re.sub(r'-\d+x\d+\.(jpg|jpeg|png|webp)$', r'.\1', image_url, flags=re.IGNORECASE)

    image_url = re.sub(r'\.(jpg|jpeg|png)\.(avif|webp)$', r'.\1', image_url, flags=re.IGNORECASE)
    if ".avif" in image_url.lower():
        clean_url = image_url.replace("https://", "").replace("http://", "")
        image_url = f"https://wsrv.nl/?url={clean_url}&output=webp"
    return image_url

def fetch_and_parse_feed(feed_info):
    print(f"Obteniendo RSS de {feed_info['fuente']}...")
    news_items = []
    try:
        response = scraper.get(feed_info["url"], timeout=15)
        if response.status_code != 200:
            print(f" -> Error {response.status_code} al obtener {feed_info['fuente']}")
            return news_items
            
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
            if item.find("enclosure") is not None:
                image_url = item.find("enclosure").get("url", "")
            elif item.find("media:content", namespaces) is not None:
                image_url = item.find("media:content", namespaces).get("url", "")

            if not image_url and content_html:
                img_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content_html, re.IGNORECASE)
                if img_match:
                    image_url = img_match.group(1)

            image_url = clean_image_url(image_url, feed_info["url"])
            cats = [cat.text.strip() for cat in item.findall("category") if cat.text]

            news_items.append({
                "id": guid, "title": title, "link": link, "published": pub_date,
                "summary": description, "content": content_html, "categories": cats,
                "image": image_url, "fuente": feed_info["fuente"]
            })
            
        con_foto = sum(1 for n in news_items if n["image"])
        print(f" -> {len(news_items)} noticias procesadas ({con_foto} con foto).")
        
    except Exception as e:
        print(f"Error en {feed_info['fuente']}: {e}")
    return news_items

def update_news():
    all_news = []
    for feed in FEEDS:
        all_news.extend(fetch_and_parse_feed(feed))

    def sort_key(item):
        try:
            return parsedate_to_datetime(item["published"]).timestamp()
        except Exception:
            return 0

    all_news.sort(key=sort_key, reverse=True)
    with open("news.json", "w", encoding="utf-8") as f:
        json.dump({"updatedAt": datetime.now().isoformat(), "totalNews": len(all_news), "news": all_news}, f, ensure_ascii=False, indent=4)
    print(f"¡{len(all_news)} noticias extraídas en total!")

if __name__ == "__main__":
    update_news()
