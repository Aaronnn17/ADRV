import json
import os
import requests

API_KEY = os.environ.get('FOOTBALL_API_KEY')
print(f"API Key presente: {bool(API_KEY)}")

headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

STADIUMS_MAP = {
    "Barcelona": "Spotify Camp Nou",
    "Atletico Madrid": "Cívitas Metropolitano",
    "Real Madrid": "Estadio Santiago Bernabéu",
    "Real Betis": "Benito Villamarín",
    "Real Sociedad": "Reale Arena",
    "Sevilla": "Ramón Sánchez-Pizjuán",
    "Athletic Club": "San Mamés",
    "Deportivo La Coruña": "Abanca-Riazor",
    "Villarreal": "Estadio de la Cerámica",
    "Celta Vigo": "Abanca-Balaídos",
    "Alaves": "Mendizorrotza",
    "Rayo Vallecano": "Estadio de Vallecas",
    "Getafe": "Coliseum",
    "Espanyol": "Stage Front Stadium",
    "Osasuna": "El Sadar",
    "Racing Santander": "Campos de Sport de El Sardinero",
    "Elche": "Estadio Martínez Valero",
    "Levante": "Ciutat de València",
    "Valencia": "Camp de Mestalla",
    "Malaga": "La Rosaleda"
}

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
            
            # Asignamos el estadio oficial mediante el diccionario, o uno por defecto si no estuviera listado
            venue_name = STADIUMS_MAP.get(home_team, f"Estadio de {home_team}")
            
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
