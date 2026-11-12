import json
import os
import re
import requests
from bs4 import BeautifulSoup
import pycountry
import unicodedata
import urllib.parse
import difflib
from playwright.sync_api import sync_playwright

def normalize(text):
    if not text: return ""
    text = unicodedata.normalize('NFKD', str(text)).encode('ASCII', 'ignore').decode('utf-8')
    return text.lower().strip()

API_KEY = os.environ.get('FOOTBALL_API_KEY')
headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

team_url = "https://api.football-data.org/v4/teams/87"
web_squad_url = "https://www.rayovallecano.es/jugadores"

try:
    # 1. API OFICIAL (football-data.org)
    response = requests.get(team_url, headers=headers, timeout=15)
    raw_squad = []
    club_info = {
        "id": 87, "name": "Rayo Vallecano", "shortName": "Rayo",
        "crest": "", "venue": "Estadio de Vallecas", "coach": "Iñigo Pérez"
    }

    if response.status_code == 200:
        data = response.json()
        club_info["id"] = data.get("id", 87)
        club_info["name"] = data.get("name", club_info["name"])
        club_info["shortName"] = data.get("shortName", club_info["shortName"])
        club_info["crest"] = data.get("crest", "")
        club_info["venue"] = data.get("venue", club_info["venue"])
        if data.get("coach", {}).get("name"):
            club_info["coach"] = data["coach"]["name"]
        raw_squad = data.get("squad", [])

    # 2. EXTRACCIÓN REAL CON PLAYWRIGHT (FOTOS + DATOS EXTRA)
    scraped_players = {}
    print(f"Abriendo navegador Chromium para cargar fotos y datos desde: {web_squad_url}...")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            viewport={"width": 1440, "height": 900}
        )
        page = context.new_page()
        page.goto(web_squad_url, wait_until="domcontentloaded", timeout=30000)
        page.wait_for_timeout(2000)

        # Hacemos scroll progresivo hasta abajo para obligar a Next.js a cargar todas las fotos (statics-maker)
        page.evaluate("""async () => {
            await new Promise((resolve) => {
                let totalHeight = 0;
                let distance = 350;
                let timer = setInterval(() => {
                    let scrollHeight = document.body.scrollHeight;
                    window.scrollBy(0, distance);
                    totalHeight += distance;
                    if (totalHeight >= scrollHeight) {
                        clearInterval(timer);
                        resolve();
                    }
                }, 150);
            });
        }""")
        page.wait_for_timeout(2500)

        # Extraemos todas las tarjetas directamente desde el DOM ya renderizado
        cards_data = page.evaluate("""() => {
            const results = [];
            const cards = document.querySelectorAll('a[class*="MkFootballSquadCard"]');
            cards.forEach(card => {
                const aria = card.getAttribute('aria-label') || '';
                const nameEl = card.querySelector('[class*="fullName"], [class*="name"]');
                const numEl = card.querySelector('[class*="number"]');
                const posEl = card.querySelector('[class*="position"]');
                const imgEl = card.querySelector('img[class*="MkFootballSquadCard__image"]') || card.querySelector('img');
                
                let photo = '';
                if (imgEl) {
                    photo = imgEl.currentSrc || imgEl.src || imgEl.getAttribute('srcset') || '';
                }

                results.push({
                    rawName: nameEl ? nameEl.innerText.trim() : aria.replace('Ver detalles de', '').trim(),
                    ariaName: aria.replace('Ver detalles de', '').trim(),
                    number: numEl ? numEl.innerText.trim() : '',
                    positionEs: posEl ? posEl.innerText.trim() : '',
                    photo: photo,
                    profileUrl: card.href || ''
                });
            });
            return results;
        }""")

        # Procesamos cada jugador extraído de la web y creamos la LISTA PRINCIPAL
        unique_web_players = []
        for item in cards_data:
            raw_name = item.get("rawName") or item.get("ariaName")
            if not raw_name:
                continue
            
            photo_url = item.get("photo", "")
            if "," in photo_url or " " in photo_url:
                photo_url = photo_url.split(",")[0].strip().split(" ")[0]

            if "/_next/image?url=" in photo_url:
                parsed_url = urllib.parse.urlparse(photo_url)
                qs = urllib.parse.parse_qs(parsed_url.query)
                if "url" in qs:
                    photo_url = qs["url"][0]

            if photo_url.startswith("data:"):
                photo_url = ""
            elif photo_url.startswith("/"):
                photo_url = f"https://www.rayovallecano.es{photo_url}"

            num_text = item.get("number", "")
            shirt_number = int(num_text) if num_text.isdigit() else None

            info_obj = {
                "displayName": raw_name,
                "shirtNumber": shirt_number,
                "positionEs": item.get("positionEs", "").upper(), # Lo guardamos ya en mayúsculas
                "photo": photo_url,
                "profileUrl": item.get("profileUrl", "")
            }
            
            unique_web_players.append(info_obj)

        browser.close()

    print(f" -> Jugadores encontrados en la web oficial: {len(unique_web_players)}.")

    # 3. FUSIÓN INVERTIDA: LA WEB MANDA (Buscamos en la API solo para rellenar huecos)
    formatted_squad = []

    # Diccionario opcional por si la web y la API tienen nombres muy distintos
    alias_web_a_api = {
        "isi": "isi palazón",
        # "nombre que sale en la web (en minusculas)": "nombre que tiene la api (en minusculas)"
    }

    for web_player in unique_web_players:
        web_name = web_player["displayName"]
        norm_web = normalize(web_name)
        
        # Aplicamos alias si existe
        search_name = alias_web_a_api.get(norm_web, norm_web)

        matched_api = {}

        # Intento 1: Buscar coincidencia en la API
        for api_player in raw_squad:
            api_name = api_player.get("name", "")
            norm_api = normalize(api_name)
            clean_web = re.sub(r'^[a-z]\.\s*', '', search_name)
            
            if search_name in norm_api or norm_api in search_name or (len(clean_web) > 3 and clean_web in norm_api):
                matched_api = api_player
                break

        # Intento 2: Emparejamiento difuso (Fuzzy matching)
        if not matched_api and raw_squad:
            api_names = {normalize(p.get("name", "")): p for p in raw_squad}
            matches = difflib.get_close_matches(search_name, api_names.keys(), n=1, cutoff=0.55)
            if matches:
                matched_api = api_names[matches[0]]

        # Extraemos el dorsal: manda la web, si no lo tiene, miramos la API
        api_shirt = matched_api.get("shirtNumber")
        web_shirt = web_player.get("shirtNumber")
        final_shirt = web_shirt if web_shirt is not None else api_shirt

        # Extraemos la nacionalidad de la API si la hemos encontrado
        nationality_raw = matched_api.get("nationality", "")
        iso_code = ""
        if nationality_raw:
            try:
                country_obj = pycountry.countries.lookup(nationality_raw)
                iso_code = country_obj.alpha_2
            except LookupError:
                iso_code = ""

        formatted_squad.append({
            "id": matched_api.get("id"), # Si es canterano y no está en la API, será null (no pasa nada)
            "name": matched_api.get("name", web_player["displayName"]),
            "displayName": web_player["displayName"],
            "position": matched_api.get("position", "Unknown"),
            "positionEs": web_player.get("positionEs", ""),
            "dateOfBirth": matched_api.get("dateOfBirth", ""),
            "nationality": nationality_raw,
            "countryCode": iso_code,
            "shirtNumber": final_shirt,
            "photo": web_player.get("photo", ""),
            "profileUrl": web_player.get("profileUrl", "")
        })

    with open("squad.json", "w", encoding="utf-8") as f:
        json.dump({"club": club_info, "squad": formatted_squad}, f, indent=4, ensure_ascii=False)

    con_foto = sum(1 for p in formatted_squad if p["photo"])
    con_dorsal = sum(1 for p in formatted_squad if p["shirtNumber"] is not None)
    print(f"¡Terminado! Jugadores totales: {len(formatted_squad)} | Con DORSAL: {con_dorsal} | Con FOTO: {con_foto}")

except Exception as e:
    print(f"Error crítico en update_squad.py: {e}")
