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
    # 1. Intentamos obtener información base de la API de football-data
    response = requests.get(team_url, headers=headers, timeout=15)
    raw_squad = []
    club_info = {
        "id": 87,
        "name": "Rayo Vallecano",
        "shortName": "Rayo",
        "crest": "",
        "venue": "Estadio de Vallecas",
        "coach": "Desconocido"
    }

    if response.status_code == 200:
        data = response.json()
        club_info = {
            "id": data.get("id"),
            "name": data.get("name"),
            "shortName": data.get("shortName"),
            "crest": data.get("crest"),
            "venue": data.get("venue"),
            "coach": data.get("coach", {}).get("name", "Desconocido")
        }
        raw_squad = data.get("squad", [])

    # 2. Hacemos Web Scraping a la web oficial del Rayo para sacar fotos y dorsales reales
    scraped_players = {}
    print(f"Obteniendo plantilla y dorsales de la web oficial: {web_squad_url}...")
    
    web_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    web_response = requests.get(web_squad_url, headers=web_headers, timeout=15)
    
    if web_response.status_code == 200:
        soup = BeautifulSoup(web_response.text, 'html.parser')
        
        # Buscamos las tarjetas de jugadores en la web (según la estructura analizada en DevTools)
        # Cada tarjeta suele tener un contenedor con el dorsal y la foto
        cards = soup.find_all(class_=lambda x: x and 'footballSquadCard' in x)
        
        for card in cards:
            try:
                # Extraer nombre
                name_elem = card.find(class_=lambda x: x and 'title' in x) or card.find('h3') or card.find('h4')
                # Si no lo halla por clase, buscamos texto alternativo o aria-label
                player_name = ""
                if name_elem:
                    player_name = name_elem.get_text(strip=True)
                else:
                    aria = card.get('aria-label', '')
                    if 'Ver detalles de' in aria:
                        player_name = aria.replace('Ver detalles de', '').strip()

                # Extraer dorsal (suele estar en un div grande de fondo o etiqueta de número)
                number_elem = card.find(class_=lambda x: x and ('number' in x or 'dorsal' in x))
                shirt_number = None
                if number_elem:
                    num_text = number_elem.get_text(strip=True)
                    if num_text.isdigit():
                        shirt_number = int(num_text)

                # Extraer foto
                img_elem = card.find('img')
                photo_url = ""
                if img_elem:
                    photo_url = img_elem.get('src') or img_elem.get('data-src', '')

                if player_name:
                    scraped_players[player_name.lower()] = {
                        "shirtNumber": shirt_number,
                        "photo": photo_url
                    }
            except Exception as e:
                print(f"Error procesando una tarjeta de jugador web: {e}")

    # 3. Fusionamos los datos de la API y de la Web Oficial
    formatted_squad = []
    
    # Si la API devolvió jugadores, los usamos como base
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
            
            # Buscamos coincidencia en lo que hemos scrapeado de la web
            scraped_info = scraped_players.get(player_name.lower(), {})
            
            # Priorizamos el dorsal y la foto de la web oficial si la API los da vacíos
            final_shirt = api_shirt if api_shirt is not None else scraped_info.get("shirtNumber")
            final_photo = scraped_info.get("photo", "")

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
        # PLAN B: Si la API fallara por completo, volcamos directamente lo scrapeado de la web
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
        
    print("squad.json generado correctamente con fotos y dorsales scrapeados.")

except Exception as e:
    print(f"Error crítico en update_squad.py: {e}")
