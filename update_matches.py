import json
import os
import requests

API_KEY = os.environ.get('FOOTBALL_API_KEY')
print(f"API Key presente: {bool(API_KEY)}")

headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

# 1. Obtener la información del club y la plantilla (ID del Rayo Vallecano = 74)
team_url = "https://api.football-data.org/v4/teams/74"
team_info = {}

try:
    print("Consultando información del club (Team)...")
    team_response = requests.get(team_url, headers=headers, timeout=15)
    
    if team_response.status_code == 200:
        team_info = team_response.json()
        print(f"Info del club obtenida con éxito. Jugadores en plantilla: {len(team_info.get('squad', []))}")
    else:
        print(f"Error al obtener info del club. Status code: {team_response.status_code}")
except Exception as e:
    print(f"Error en la petición de info del club: {e}")

# 2. Obtener el calendario de partidos (Matches)
# Filtramos por el equipo (74) para asegurar que solo traemos sus partidos
matches_url = "https://api.football-data.org/v4/teams/74/matches"
querystring = {"season": "2026"}
formatted_matches = []

try:
    print("Consultando calendario de partidos (Matches)...")
    matches_response = requests.get(matches_url, headers=headers, params=querystring, timeout=15)
    
    if matches_response.status_code == 200:
        matches_data = matches_response.json()
        matches = matches_data.get("matches", [])
        print(f"Partidos encontrados en la API: {len(matches)}")
        
        for match in matches:
            home_team = match.get("homeTeam", {}).get("name", "")
            away_team = match.get("awayTeam", {}).get("name", "")
            score = match.get("score", {}).get("fullTime", {})
            status_api = match.get("status", "SCHEDULED")
            
            status = "FINISHED" if status_api == "FINISHED" else "UPCOMING"
            
            # Obtener datos de la competición de este partido específico
            competition_info = match.get("competition", {})
            competition_name = competition_info.get("name", "LaLiga EA Sports")
            
            match_obj = {
                "id": f"match_{match.get('id', '')}",
                "homeTeam": home_team,
                "awayTeam": away_team,
                "homeScore": score.get("home"),
                "awayScore": score.get("away"),
                "status": status,
                "dateText": match.get("utcDate", "Próximo partido"),
                "competition": competition_name,
                "round": f"Jornada {match.get('matchday', '')}",
                "homeLogoUrl": match.get("homeTeam", {}).get("crest", ""),
                "awayLogoUrl": match.get("awayTeam", {}).get("crest", ""),
                "matchUrl": "https://www.rayovallecano.es/partidos",
                # Añadimos más datos extraídos de la API si están disponibles
                "venue": team_info.get("venue", "Estadio de Vallecas") if "Rayo" in home_team else "" 
            }
            formatted_matches.append(match_obj)
    else:
        print(f"Error al obtener partidos. Status code: {matches_response.status_code}")

except Exception as e:
    print(f"Error en la petición de partidos: {e}")

# 3. Ensamblar el JSON final
final_output = {
    "metadata": {
        "temporada": "2026/2027",
        "actualizado_en": "GitHub Actions",
    },
    "club": {
        "id": team_info.get("id"),
        "nombre": team_info.get("name", "Rayo Vallecano"),
        "nombre_corto": team_info.get("shortName", "Rayo"),
        "fundacion": team_info.get("founded"),
        "colores": team_info.get("clubColors"),
        "estadio": team_info.get("venue"),
        "direccion": team_info.get("address"),
        "web": team_info.get("website"),
        "escudo": team_info.get("crest"),
        "entrenador": team_info.get("coach", {}).get("name"),
        "competiciones_activas": [comp.get("name") for comp in team_info.get("runningCompetitions", [])],
        "plantilla": team_info.get("squad", [])
    },
    "partidos": formatted_matches
}

# 4. Guardar el archivo JSON
try:
    with open("matches.json", "w", encoding="utf-8") as f:
        json.dump(final_output, f, indent=4, ensure_ascii=False)
    print(f"¡matches.json actualizado con éxito! Incluye {len(formatted_matches)} partidos y la info del club.")
except Exception as e:
    print(f"Error al escribir el archivo JSON: {e}")
    exit(1)
