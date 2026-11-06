import json
import os
import requests
import pycountry

API_KEY = os.environ.get('FOOTBALL_API_KEY')
headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

team_url = "https://api.football-data.org/v4/teams/87"

try:
    response = requests.get(team_url, headers=headers, timeout=15)
    
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
        formatted_squad = []
        for player in raw_squad:
            nationality_raw = player.get("nationality", "")
            iso_code = ""
            
            if nationality_raw:
                try:
                    # Intentamos buscar el país por su nombre común (ej. "Indonesia", "Spain")
                    country_obj = pycountry.countries.lookup(nationality_raw)
                    iso_code = country_obj.alpha_2 # Devuelve "ID", "ES", etc.
                except LookupError:
                    # Si pycountry no lo encuentra exacto, probamos a buscar por nombre en español o aproximado si fuera necesario
                    iso_code = ""

            formatted_squad.append({
                "id": player.get("id"),
                "name": player.get("name"),
                "position": player.get("position", "Unknown"),
                "nationality": nationality_raw,
                "countryCode": iso_code,
                "shirtNumber": player.get("shirtNumber")
            })

        final_output = {
            "club": club_info,
            "squad": formatted_squad
        }

        with open("squad.json", "w", encoding="utf-8") as f:
            json.dump(final_output, f, indent=4, ensure_ascii=False)
            
        print("squad.json creado con conversión automática de países.")

    else:
        print(f"Error al obtener la plantilla. Code: {response.status_code}")

except Exception as e:
    print(f"Error en la ejecución: {e}")
