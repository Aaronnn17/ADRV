import json
import os
import requests
from bs4 import BeautifulSoup
import pycountry
import unicodedata
import urllib.parse
import difflib

# Función para quitar tildes y mayúsculas y comparar nombres sin fallos
def normalize(text):
    if not text: return ""
    text = unicodedata.normalize('NFKD', text).encode('ASCII', 'ignore').decode('utf-8')
    return text.lower().strip()

API_KEY = os.environ.get('FOOTBALL_API_KEY')
headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

team_url = "https://api.football-data.org/v4/teams/87"
web_squad_url = "https://www.rayovallecano.es/jugadores"

try:
    # 1. API OFICIAL
    response = requests.get(team_url, headers=headers, timeout=15)
    raw_squad = []
    club_info = {
        "id": 87, "name": "Rayo Vallecano", "shortName": "Rayo",
        "crest": "", "venue": "Estadio de Vallecas", "coach": "Beñat San José"
    }

    if response.status_code == 200:
        data = response.json()
        club_info["id"] = data.get("id", 87)
        club_info["name"] = data.get("name", club_info["name"])
        club_info["shortName"] = data.get("shortName", club_info["shortName"])
        club_info["crest"] = data.get("crest", "")
        club_info["venue"] = data.get("venue", club_info["venue"])
        raw_squad = data.get("squad", [])

    # 2. WEB SCRAPING AVANZADO
    scraped_players = {}
    print(f"Extrayendo datos esquivando el Lazy Loading desde: {web_squad_url}...")
    
    web_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    web_response = requests.get(web_squad_url, headers=web_headers, timeout=15)
    
    if web_response.status_code == 200:
        soup = BeautifulSoup(web_response.text, 'html.parser')
        
        # Atacamos directamente al enlace que envuelve al jugador (Siempre es seguro)
        links = soup.find_all('a', attrs={'aria-label': lambda x: x and 'Ver detalles de' in x})
        
        for link in links:
            raw_name = link['aria-label'].replace('Ver detalles de', '').strip()
            norm_name = normalize(raw_name)
            
            # --- FOTO: CAZANDO statics-maker ---
            photo_url = ""
            
            # 1. Primero miramos si está encriptada dentro de un <noscript> de Next.js
            noscript = link.find('noscript')
            if noscript:
                img = noscript.find('img')
                if img and img.get('src') and 'statics-maker' in img.get('src'): 
                    photo_url = img.get('src')
            
            # 2. Si no, revisamos todas las imágenes dentro de la tarjeta
            if not photo_url:
                imgs = link.find_all('img')
                for img in imgs:
                    # Comprobamos el srcset (donde suelen estar las de alta calidad)
                    if img.get('srcset') and 'statics-maker' in img.get('srcset'):
                        photo_url = img.get('srcset').split(',')[0].strip().split(' ')[0]
                        break
                    # Si no, comprobamos el src normal
                    elif img.get('src') and 'statics-maker' in img.get('src'):
                        photo_url = img.get('src')
                        break
            
            # Limpiar URL encriptada de Next.js (_next/image?url=...) si la tuviera
            if '/_next/image?url=' in photo_url:
                parsed_url = urllib.parse.urlparse(photo_url)
                qs = urllib.parse.parse_qs(parsed_url.query)
                if 'url' in qs:
                    photo_url = qs['url'][0]

            # Por si acaso la página devuelve una ruta relativa (aunque statics-maker suele ser absoluta)
            if photo_url.startswith('/'):
                photo_url = f"https://www.rayovallecano.es{photo_url}"

            # --- DORSAL ---
            number_elem = link.find(class_=lambda x: x and 'number' in x)
            shirt_number = None
            if number_elem:
                num_text = number_elem.get_text(strip=True)
                if num_text.isdigit(): shirt_number = int(num_text)

            scraped_players[norm_name] = {
                "shirtNumber": shirt_number,
                "photo": photo_url
            }

    # 3. EMPAREJAMIENTO INTELIGENTE (FUZZY MATCHING)
    formatted_squad = []
    
    for player in raw_squad:
        api_name = player.get("name", "")
        norm_api_name = normalize(api_name)
        api_shirt = player.get("shirtNumber")
        
        matched_info = {}
        
        # Intento 1: Ver si un nombre está dentro del otro (Ej: "ciss" dentro de "pathe ciss")
        for web_name, info in scraped_players.items():
            if web_name in norm_api_name or norm_api_name in web_name:
                matched_info = info
                break
        
        # Intento 2: Emparejamiento difuso por similitud (Ej: ntekja vs nteka)
        if not matched_info and scraped_players:
            matches = difflib.get_close_matches(norm_api_name, scraped_players.keys(), n=1, cutoff=0.4)
            if matches:
                matched_info = scraped_players[matches[0]]

        # Juntamos los datos priorizando que haya dorsal
        final_shirt = api_shirt if (api_shirt is not None and api_shirt > 0) else matched_info.get("shirtNumber")
        final_photo = matched_info.get("photo", "")
        
        nationality_raw = player.get("nationality", "")
        iso_code = ""
        if nationality_raw:
            try:
                country_obj = pycountry.countries.lookup(nationality_raw)
                iso_code = country_obj.alpha_2
            except LookupError:
                iso_code = ""

        formatted_squad.append({
            "id": player.get("id"),
            "name": api_name,
            "position": player.get("position", "Unknown"),
            "nationality": nationality_raw,
            "countryCode": iso_code,
            "shirtNumber": final_shirt,
            "photo": final_photo
        })

    with open("squad.json", "w", encoding="utf-8") as f:
        json.dump({ "club": club_info, "squad": formatted_squad }, f, indent=4, ensure_ascii=False)
        
    print(f"¡Terminado! Jugadores guardados con FOTO y DORSAL: {sum(1 for p in formatted_squad if p['photo'])}")

except Exception as e:
    print(f"Error crítico en update_squad.py: {e}")
