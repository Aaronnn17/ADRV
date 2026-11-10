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
    # 1. Obtenemos la base de datos oficial de la API como respaldo
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

    # 2. Scraping directo de los contenedores MkFootballSquadCard de la web del Rayo
    scraped_players = {}
    print(f"Extrayendo tarjetas de jugadores desde: {web_squad_url}...")
    
    web_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    web_response = requests.get(web_squad_url, headers=web_headers, timeout=15)
    
    if web_response.status_code == 200:
        soup = BeautifulSoup(web_response.text, 'html.parser')
        
        # Buscamos todas las tarjetas que contengan 'MkFootballSquadCard' en sus clases
        cards = soup.find_all(lambda tag: tag.has_attr('class') and any('MkFootballSquadCard' in c for c in tag['class']))
        
        for card in cards:
            try:
                # Nos aseguramos de coger la tarjeta principal de cada jugador
                classes = card.get('class', [])
                if not any(c == 'MkFootballSquadCard' for c in classes):
                    continue

                # Nombre completo del jugador (MkFootballSquadCard_fullName)
                name_elem = card.find(class_=lambda x: x and 'fullName' in x)
                player_name = name_elem.get_text(strip=True) if name_elem else ""

                # Dorsal (MkFootballSquadCard_number)
                number_elem = card.find(class_=lambda x: x and 'number' in x)
                shirt_number = None
                if number_elem:
                    num_text = number_elem.get_text(strip=True)
                    if num_text.isdigit():
                        shirt_number = int(num_text)

                # Posición (MkFootballSquadCard_position)
                position_elem = card.find(class_=lambda x: x and 'position' in x)
                position = position_elem.get_text(strip=True) if position_elem else ""

                # Foto de alta calidad dentro de la tarjeta
                img_elem = card.find('img')
                photo_url = ""
                if img_elem:
                    photo_url = img_elem.get('src') or img_elem.get('data-src', '')

                if player_name:
                    scraped_players[player_name.lower()] = {
                        "shirtNumber": shirt_number,
                        "position": position,
                        "photo": photo_url
                    }
            except Exception as e:
                print(f"Error procesando tarjeta individual: {e}")

    # 3. Fusionamos la lista oficial de la API con los datos precisos de las tarjetas web
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
            
            # Buscamos coincidencia flexible con los datos extraídos de las tarjetas
            matched_info = {}
            for key, info in scraped_players.items():
                if key in player_name.lower() or player_name.lower() in key:
                    matched_info = info
                    break

            final_shirt = api_shirt if (api_shirt is not None and api_shirt > 0) else matched_info.get("shirtNumber")
            final_photo = matched_info.get("photo", "")
            final_position = player.get("position") if player.get("position") != "Unknown" else (matched_info.get("position") or "Unknown")

            formatted_squad.append({
                "id": player.get("id"),
                "name": player_name,
                "position": final_position,
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
                "position": info.get("position", "Unknown"),
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
        
    print(f"squad.json generado con éxito. Jugadores con foto: {sum(1 for p in formatted_squad if p['photo'])}")

except Exception as e:
    print(f"Error crítico en update_squad.py: {e}")
