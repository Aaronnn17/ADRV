import json
import os
import requests
from bs4 import BeautifulSoup
import pycountry

API_KEY = os.environ.get('FOOTBALL_API_KEY')
headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

team_url = "https://api.football-data.org/v4/teams/87"
web_squad_url = "https://www.rayovallecano.es/jugadores"

try:
    # 1. Obtenemos la base de datos oficial (posiciones, nacionalidades, etc.)
    response = requests.get(team_url, headers=headers, timeout=15)
    raw_squad = []
    club_info = {
        "id": 87,
        "name": "Rayo Vallecano",
        "shortName": "Rayo",
        "crest": "",
        "venue": "Estadio de Vallecas",
        "coach": "Beñat San José"
    }

    if response.status_code == 200:
        data = response.json()
        club_info = {
            "id": data.get("id"),
            "name": data.get("name"),
            "shortName": data.get("shortName"),
            "crest": data.get("crest"),
            "venue": data.get("venue"),
            "coach": data.get("coach", {}).get("name", "Beñat San José")
        }
        raw_squad = data.get("squad", [])

    # 2. Scraping quirúrgico de la web del Rayo basado en las tarjetas de jugadores
    scraped_players = {}
    print(f"Extrayendo fotos y dorsales de la web oficial: {web_squad_url}...")
    
    web_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    web_response = requests.get(web_squad_url, headers=web_headers, timeout=15)
    
    if web_response.status_code == 200:
        soup = BeautifulSoup(web_response.text, 'html.parser')
        
        # Localizamos todas las tarjetas de jugadores mediante el aria-label exacto que vimos en las DevTools
        cards = soup.find_all(lambda tag: tag.has_attr('aria-label') and 'Ver detalles de' in tag['aria-label'])
        
        for card in cards:
            try:
                # Extraer nombre del aria-label (ej. "Ver detalles de Cárdenas" -> "Cárdenas")
                aria = card.get('aria-label', '')
                raw_name = aria.replace('Ver detalles de', '').strip()
                
                # Extraer la foto de alta calidad de la etiqueta img dentro de la tarjeta
                img_elem = card.find('img')
                photo_url = ""
                if img_elem:
                    photo_url = img_elem.get('src') or img_elem.get('data-src', '')
                    # Si la URL es relativa, la hacemos absoluta
                    if photo_url.startswith('/'):
                        photo_url = f"https://www.rayovallecano.es{photo_url}"

                # Extraer el dorsal buscando en el bloque de texto o contenedores hermanos/superiores de la tarjeta
                shirt_number = None
                parent_block = card.find_parent(class_=lambda x: x and 'grid' in x.lower()) or card.parent
                if parent_block:
                    import re
                    # Buscamos números de dorsal típicos (1 al 99) que suelan aparecer en el fondo de la tarjeta
                    text_block = parent_block.get_text(" ", strip=True)
                    numbers = re.findall(r'\b([1-9]|[1-9][0-9])\b', text_block)
                    if numbers:
                        # Cogemos el primer número razonable que aparezca asociado
                        shirt_number = int(numbers[0])

                if raw_name:
                    scraped_players[raw_name.lower()] = {
                        "shirtNumber": shirt_number,
                        "photo": photo_url
                    }
            except Exception as e:
                print(f"Error procesando tarjeta individual: {e}")

    # 3. Fusionamos de forma inteligente la información de la API y de la Web Oficial
    formatted_squad = []
    
    if raw_squad:
        for player in raw_squad:
            nationality_raw = player.get("nationality", "")
            iso_code = ""
            if nationality_raw:
                try:
                    country_obj = pycountry.countries.lookup(nationality_raw)
                    iso_code = country_obj.alpha_2
                except LookupError:
                    iso_code = ""

            player_name = player.get("name", "")
            api_shirt = player.get("shirtNumber")
            
            # Buscamos si coincide el apellido o nombre con los datos scrapeados de la web
            matched_info = {}
            for key, info in scraped_players.items():
                if key in player_name.lower() or player_name.lower() in key:
                    matched_info = info
                    break

            final_shirt = api_shirt if api_shirt is not None and api_shirt > 0 else matched_info.get("shirtNumber")
            final_photo = matched_info.get("photo", "")

            formatted_squad.append({
                "id": player.get("id"),
                "name": player_name,
                "position": player.get("position", "Unknown"),
                "nationality": nationality_raw,
                "countryCode": iso_code,
                "shirtNumber": final_shirt,
                "photo": final_photo
            })
    else:
        # Plan B por si la API fallara
        for name, info in scraped_players.items():
            formatted_squad.append({
                "id": hash(name),
                "name": name.title(),
                "position": "Unknown",
                "nationality": "Spain",
                "countryCode": "ES",
                "shirtNumber": info.get("shirtNumber"),
                "photo": info.get("photo")
            })

    final_output = {
        "club": club_info,
        "squad": formatted_squad
    }

    with open("squad.json", "w", encoding="utf-8") as f:
        json.dump(final_output, f, indent=4, ensure_ascii=False)
        
    print("squad.json actualizado con éxito extrayendo fotos y dorsales por completo.")

except Exception as e:
    print(f"Error crítico en update_squad.py: {e}")
