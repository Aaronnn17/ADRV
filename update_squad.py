import json
import os
import requests

API_KEY = os.environ.get('FOOTBALL_API_KEY')
headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

# ID 87 corresponde al Rayo Vallecano en la API de football-data.org
team_url = "https://api.football-data.org/v4/teams/87"

try:
    response = requests.get(team_url, headers=headers, timeout=15)
    
    if response.status_code == 200:
        data = response.json()
        
        # Extraemos los datos principales del club
        club_info = {
            "id": data.get("id"),
            "name": data.get("name"),
            "shortName": data.get("shortName"),
            "crest": data.get("crest"),
            "venue": data.get("venue"),
            "coach": data.get("coach", {}).get("name", "Desconocido")
        }
        
        # Procesamos la plantilla de jugadores
        raw_squad = data.get("squad", [])
        formatted_squad = []
        for player in raw_squad:
            formatted_squad.append({
                "id": player.get("id"),
                "name": player.get("name"),
                "position": player.get("position", "Unknown"),
                "nationality": player.get("nationality", "Desconocida"),
                "shirtNumber": player.get("shirtNumber")
            })

        final_output = {
            "club": club_info,
            "squad": formatted_squad
        }

        with open("squad.json", "w", encoding="utf-8") as f:
            json.dump(final_output, f, indent=4, ensure_ascii=False)
            
        print("squad.json creado con éxito.")

    else:
        print(f"Error al obtener la plantilla. Code: {response.status_code}")

except Exception as e:
    print(f"Error en la ejecución: {e}")
