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
    # 1. Obtenemos la base de datos de la API (por si necesitamos posiciones o nacionalidades)
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

    # 2. Scraping preciso basado en las clases que hemos visto en tus DevTools
    scraped_players = {}
    print(f"Extrayendo datos reales de la plantilla desde: {web_squad_url}...")
    
    web_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    web_response = requests.get(web_squad_url, headers=web_headers, timeout=15)
    
    if web_response.status_code == 200:
        soup = BeautifulSoup(web_response.text, 'html.parser')
        
        # Buscamos exactamente las tarjetas de jugadores que aparecen en tu captura (MkFootballSquadCard)
        cards = soup.find_all(lambda tag: tag.has_attr('aria-label') and 'Ver detalles de' in tag['aria-label'])
        
        for card in cards:
            try:
                # Extraer nombre directamente del aria-label (ej: "Ver detalles de Cárdenas" -> "Cárdenas")
                aria = card.get('aria-label', '')
                raw_name = aria.replace('Ver detalles de', '').strip()
                
                # Extraer la foto de la etiqueta img dentro de la tarjeta
                img_elem = card.find('img')
                photo_url = ""
                if img_elem:
                    photo_url = img_elem.get('src') or img_elem.get('data-src', '')
                
                # Buscar el dorsal dentro de la tarjeta (si la web lo incluye en algún span o contenedor de texto)
                card_text = card.get_text(" ", strip=True)
                import re
                numbers_found = re.findall(r'\b([1-9]|[1-9][0-9])\b', card_text)
                shirt_number = int(numbers_found[0]) if numbers_found else None

                if raw_name:
                    scraped_players[raw_name.lower()] = {
                        "shirtNumber": shirt_number,
                        "photo": photo_url
                    }
            except Exception as e:
                print(f"Error procesando una tarjeta: {e}")

    # 3. Fusionamos los datos de la API oficial con lo que hemos extraído de la web
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
            
            # Buscamos coincidencia con los nombres extraídos de la web
            matched_info = {}
            for key, info in scraped_players.items():
                if key in player_name.lower() or player_name.lower() in key:
                    matched_info = info
                    break

            final_shirt = api_shirt if api_shirt is not None else matched_info.get("shirtNumber")
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
        # Plan B por si la API no respondiera
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
        
    print("squad.json generado a la perfección con la estructura de la web oficial.")

except Exception as e:
    print(f"Error crítico en update_squad.py: {e}")
