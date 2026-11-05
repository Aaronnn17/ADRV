import json
import os
import requests

API_KEY = os.environ.get('FOOTBALL_API_KEY')
print(f"API Key presente: {bool(API_KEY)}")

# Endpoint oficial de la v4 para la competición de LaLiga (PD) en 2026
url = "https://api.football-data.org/v4/competitions/PD/matches"
querystring = {"season": "2026"}
headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

try:
    response = requests.get(url, headers=headers, params=querystring, timeout=15)
    print(f"Código de respuesta HTTP: {response.status_code}")
    
    raw_data = response.json()
    
    # Comprobamos si hay algún mensaje de error de la API
    if "message" in raw_data:
        print(f"Aviso de la API: {raw_data['message']}")

    matches = raw_data.get("matches", [])
    print(f"Total partidos en LaLiga encontrados: {len(matches)}")
    
    formatted_matches = []
    for match in matches:
        home_team = match.get("homeTeam", {}).get("name", "")
        away_team = match.get("awayTeam", {}).get("name", "")
        
        # Filtramos únicamente los partidos del Rayo Vallecano
        if "Rayo" in home_team or "Rayo" in away_team:
            score = match.get("score", {}).get("fullTime", {})
            status_api = match.get("status", "SCHEDULED")
            status = "FINISHED" if status_api == "FINISHED" else "UPCOMING"
            
            match_obj = {
                "id": f"match_{match.get('id', '')}",
                "homeTeam": home_team,
                "awayTeam": away_team,
                "homeScore": score.get("home"),
                "awayScore": score.get("away"),
                "status": status,
                "dateText": match.get("utcDate", "Próximo partido"),
                "competition": "LaLiga EA Sports",
                "round": f"Jornada {match.get('matchday', '')}",
                "homeLogoUrl": match.get("homeTeam", {}).get("crest", ""),
                "awayLogoUrl": match.get("awayTeam", {}).get("crest", ""),
                "matchUrl": "https://www.rayovallecano.es/partidos"
            }
            formatted_matches.append(match_obj)

    print(f"Partidos del Rayo Vallecano filtrados: {len(formatted_matches)}")

    final_output = {
        "temporada": "2026/2027",
        "equipo_principal": "Rayo Vallecano",
        "partidos": formatted_matches
    }

    with open("matches.json", "w", encoding="utf-8") as f:
        json.dump(final_output, f, indent=4, ensure_ascii=False)
        
    print("¡matches.json actualizado con éxito!")

except Exception as e:
    print(f"Error crítico: {e}")
    exit(1)
