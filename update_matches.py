import json
import os
import requests

API_KEY = os.environ.get('FOOTBALL_API_KEY')
print(f"API Key presente: {bool(API_KEY)}")

# URL específica de Fútbol (v3) para el Rayo Vallecano (team=728) y temporada actual (2026)
url = "https://v3.football.api-sports.io/fixtures"

querystring = {"team": "728", "season": "2026"}

headers = {
    'x-apisports-key': API_KEY
}

try:
    response = requests.get(url, headers=headers, params=querystring)
    print(f"Código de respuesta HTTP: {response.status_code}")
    
    raw_data = response.json()
    
    # Comprobamos si hay errores devueltos por la API
    errors = raw_data.get("errors", {})
    if errors:
        print(f"La API ha devuelto un error: {errors}")
        exit(1)

    fixtures = raw_data.get("response", [])
    print(f"Partidos encontrados en la API: {len(fixtures)}")
    
    formatted_matches = []

    for item in fixtures:
        fixture = item.get("fixture", {})
        teams = item.get("teams", {})
        goals = item.get("goals", {})
        league = item.get("league", {})

        home_team = teams.get("home", {}).get("name", "Rayo Vallecano")
        away_team = teams.get("away", {}).get("name", "Rival")
        
        home_logo = teams.get("home", {}).get("logo", "")
        away_logo = teams.get("away", {}).get("logo", "")
        
        status_short = fixture.get("status", {}).get("short", "NS")
        status = "FINISHED" if status_short in ["FT", "AET", "PEN"] else "UPCOMING"
        
        match_date = fixture.get("date", "Próximo partido")
        competition = league.get("name", "LaLiga EA Sports")
        round_name = league.get("round", "Estadio de Vallecas")

        match_obj = {
            "id": f"match_{fixture.get('id', '')}",
            "homeTeam": home_team,
            "awayTeam": away_team,
            "homeScore": goals.get("home"),
            "awayScore": goals.get("away"),
            "status": status,
            "dateText": match_date,
            "competition": competition,
            "round": round_name,
            "homeLogoUrl": home_logo,
            "awayLogoUrl": away_logo,
            "matchUrl": "https://www.rayovallecano.es/partidos"
        }
        formatted_matches.append(match_obj)

    final_output = {
        "temporada": "2026/2027",
        "equipo_principal": "Rayo Vallecano",
        "partidos": formatted_matches
    }

    with open("matches.json", "w", encoding="utf-8") as f:
        json.dump(final_output, f, indent=4, ensure_ascii=False)
        
    print("¡matches.json actualizado correctamente con los partidos de la API!")

except Exception as e:
    print(f"Error crítico al conectar con la API: {e}")
    exit(1)
