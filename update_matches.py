import json
import os
import requests

API_KEY = os.environ.get('FOOTBALL_API_KEY')
print(f"API Key presente: {bool(API_KEY)}")

headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

team_url = "https://api.football-data.org/v4/teams/87"
team_info = {}
try:
    team_response = requests.get(team_url, headers=headers, timeout=15)
    if team_response.status_code == 200:
        team_info = team_response.json()
except Exception as e:
    print(f"Error: {e}")

matches_url = "https://api.football-data.org/v4/teams/87/matches"
querystring = {"season": "2026"}
formatted_matches = []

try:
    matches_response = requests.get(matches_url, headers=headers, params=querystring, timeout=15)
    if matches_response.status_code == 200:
        matches = matches_response.json().get("matches", [])
        
        for match in matches:
            home_team = match.get("homeTeam", {}).get("name", "")
            away_team = match.get("awayTeam", {}).get("name", "")
            
            # Lógica corregida para el estadio:
            # 1. Si la API proporciona un campo de estadio en el equipo local o en el partido
            # 2. Si el Rayo Vallecano juega como local ("Rayo Vallecano" en home_team), es el Estadio de Vallecas.
            # 3. Si juega fuera, asignamos el estadio del equipo local o un campo visitante.
            home_team_obj = match.get("homeTeam", {})
            api_venue = match.get("venue") or home_team_obj.get("venue")

            if api_venue:
                venue_name = api_venue
            elif "Rayo Vallecano" in home_team:
                venue_name = "Estadio de Vallecas"
            else:
                # Si juega fuera, usamos el nombre del estadio del equipo local si lo hay, o el nombre del equipo local
                venue_name = f"Estadio de {home_team}"
            
            score = match.get("score", {})
            full_time = score.get("fullTime", {})
            
            referees = match.get("referees", [])
            main_referee = referees[0].get("name", "Por designar") if referees else "Por designar"
            
            status_api = match.get("status", "SCHEDULED")
            competition_name = match.get("competition", {}).get("name", "LaLiga")
            
            match_obj = {
                "id": f"match_{match.get('id', '')}",
                "homeTeam": home_team,
                "awayTeam": away_team,
                "homeScore": full_time.get("home"),
                "awayScore": full_time.get("away"),
                "status": "FINISHED" if status_api == "FINISHED" else "UPCOMING",
                "statusApi": status_api,
                "dateText": match.get("utcDate", "Próximo partido"),
                "competition": competition_name,
                "round": f"Jornada {match.get('matchday', '')}",
                "homeLogoUrl": match.get("homeTeam", {}).get("crest", ""),
                "awayLogoUrl": match.get("awayTeam", {}).get("crest", ""),
                "venue": venue_name,
                "referee": main_referee
            }
            formatted_matches.append(match_obj)
except Exception as e:
    print(f"Error: {e}")

final_output = {
    "metadata": {"temporada": "2026/2027"},
    "club": {}, 
    "partidos": formatted_matches
}

with open("matches.json", "w", encoding="utf-8") as f:
    json.dump(final_output, f, indent=4, ensure_ascii=False)
