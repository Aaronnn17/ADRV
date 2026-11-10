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
    # 1. Obtenemos datos base de la API de football-data
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

    # 2. Scraping mejorado de la web oficial del Rayo Vallecano
    scraped_players = {}
    print(f"Buscando fotos y dorsales en: {web_squad_url}...")
    
    web_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    web_response = requests.get(web_squad_url, headers=web_headers, timeout=15)
    
    if web_response.status_code == 200:
        soup = BeautifulSoup(web_response.text, 'html.parser')
        
        # Buscamos enlaces o bloques que contengan información de jugadores
        # Analizamos cualquier elemento que tenga una imagen de jugador y un título o aria-label
        cards = soup.find_all(['div', 'article', 'li'], class_=lambda x: x and any(term in x.lower() for term in ['squad', 'player', 'card', 'item']))
        
        if not cards:
            # Plan B: buscamos todas las imágenes con sus contenedores si no hay clases específicas
            cards = soup.find_all('img')

        for card in cards:
            try:
                # Si estamos procesando etiquetas img directamente o tarjetas
                if card.name == 'img':
                    img_elem = card
                    parent = card.find_parent(['div', 'article', 'a'])
                    text_content = parent.get_text(" ", strip=True) if parent else ""
                else:
                    img_elem = card.find('img')
                    text_content = card.get_text(" ", strip=True)

                if not img_elem:
                    continue

                photo_url = img_elem.get('src') or img_elem.get('data-src', '')
                if not photo_url or "logo" in photo_url.lower():
                    continue

                # Normalizar URL de la foto si es relativa
                if photo_url.startswith('/'):
                    photo_url = f"https://www.rayovallecano.es{photo_url}"

                # Intentar extraer el dorsal (números sueltos de 1 o 2 dígitos en el texto)
                import re
                numbers_found = re.findall(r'\b([1-9]|[1-9][0-9])\b', text_content)
                shirt_number = int(numbers_found[0]) if numbers_found else None

                # Limpiar el nombre del jugador del texto encontrado
                # (Eliminamos palabras clave como 'Portero', 'Defensa', 'Centrocampista', 'Delantero')
                clean_name = text_content
                for pos_word in ['Portero', 'Defensa', 'Centrocampista', 'Delantero', 'Goalkeeper', 'Defender', 'Midfielder', 'Forward']:
                    clean_name = clean_name.replace(pos_word, '')
                
                # Quitamos números aislados
                clean_name = re.sub(r'\b\d+\b', '', clean_name).strip()
                
                # Intentamos acotar un nombre razonable (primeras 2 o 3 palabras)
                name_words = clean_name.split()
                if name_words:
                    # Buscamos si coincide con algún jugador de la lista de la API
                    player_name_candidate = " ".join(name_words[:2]).title()
                    
                    scraped_players[player_name_candidate.lower()] = {
                        "shirtNumber": shirt_number,
                        "photo": photo_url
                    }
            except Exception as e:
                pass

    # 3. Fusionamos los datos con la API oficial
    formatted_squad = []
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
        
        # Buscamos coincidencia parcial por nombre
        matched_info = {}
        for key, info in scraped_players.items():
            if any(part.lower() in player_name.lower() for part in key.split()):
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

    final_output = {
        "club": club_info,
        "squad": formatted_squad
    }

    with open("squad.json", "w", encoding="utf-8") as f:
        json.dump(final_output, f, indent=4, ensure_ascii=False)
        
    print("squad.json actualizado con éxito mediante scraping web.")

except Exception as e:
    print(f"Error en update_squad.py: {e}")
