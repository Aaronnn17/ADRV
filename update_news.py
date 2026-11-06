import urllib.request
import xml.etree.ElementTree as ET
import json
from datetime import datetime

RSS_URL = "https://www.rayovallecano.es/rss"

def update_news():
    print("Obteniendo RSS oficial del Rayo Vallecano...")
    try:
        req = urllib.request.Request(
            RSS_URL, 
            headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
        )
        with urllib.request.urlopen(req) as response:
            xml_data = response.read()
    except Exception as e:
        print(f"Error al descargar el RSS: {e}")
        return

    root = ET.fromstring(xml_data)
    channel = root.find("channel")
    
    news_list = []
    
    # Namespaces definidos en el XML del RSS para capturar content:encoded
    namespaces = {
        'content': 'http://purl.org/rss/1.0/modules/content/'
    }
    
    if channel is not None:
        # Recorremos TODOS los items sin excepción
        for item in channel.findall("item"):
            title_elem = item.find("title")
            title = title_elem.text.strip() if title_elem is not None and title_elem.text else ""
            
            link_elem = item.find("link")
            link = link_elem.text.strip() if link_elem is not None and link_elem.text else ""
            
            pub_date_elem = item.find("pubDate")
            pub_date = pub_date_elem.text.strip() if pub_date_elem is not None and pub_date_elem.text else ""
            
            guid_elem = item.find("guid")
            guid = guid_elem.text.strip() if guid_elem is not None and guid_elem.text else link
            
            # Imagen desde <enclosure>
            image_url = ""
            enclosure = item.find("enclosure")
            if enclosure is not None:
                image_url = enclosure.get("url", "")

            # Descripción corta (<description>)
            description = ""
            desc_tag = item.find("description")
            if desc_tag is not None and desc_tag.text:
                description = desc_tag.text.strip()

            # Contenido completo (<content:encoded>)
            content_html = ""
            content_elem = item.find("content:encoded", namespaces)
            if content_elem is not None and content_elem.text:
                content_html = content_elem.text.strip()

            # Categorías (puede haber varias)
            categories = [cat.text.strip() for cat in item.findall("category") if cat.text]

            news_list.append({
                "id": guid,
                "title": title,
                "link": link,
                "published": pub_date,
                "summary": description,
                "content": content_html,
                "categories": categories,
                "image": image_url
            })

    data = {
        "updatedAt": datetime.now().isoformat(),
        "totalNews": len(news_list),
        "news": news_list
    }

    with open("news.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
    
    print(f"¡{len(news_list)} noticias extraídas con todos sus datos y guardadas en news.json!")

if __name__ == "__main__":
    update_news()
