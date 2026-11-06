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

    # Parsear el documento XML del RSS
    root = ET.fromstring(xml_data)
    channel = root.find("channel")
    
    news_list = []
    
    if channel is not None:
        for item in channel.findall("item"):
            title = item.find("title").text.strip() if item.find("title") is not None else ""
            link = item.find("link").text.strip() if item.find("link") is not None else ""
            pub_date = item.find("pubDate").text.strip() if item.find("pubDate") is not None else ""
            guid = item.find("guid").text.strip() if item.find("guid") is not None else link
            
            # Extraer la imagen de la etiqueta <enclosure>
            image_url = ""
            enclosure = item.find("enclosure")
            if enclosure is not None:
                image_url = enclosure.get("url", "")

            # Extraer descripción o contenido
            description = ""
            desc_tag = item.find("description")
            if desc_tag is not None and desc_tag.text:
                description = desc_tag.text.strip()

            news_list.append({
                "id": guid,
                "title": title,
                "link": link,
                "published": pub_date,
                "summary": description,
                "image": image_url
            })

    data = {
        "updatedAt": datetime.now().isoformat(),
        "news": news_list
    }

    with open("news.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
    
    print(f"¡{len(news_list)} noticias extraídas y guardadas en news.json con éxito!")

if __name__ == "__main__":
    update_news()
