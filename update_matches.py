import json
import os
import urllib.request

API_KEY = os.environ.get('FOOTBALL_API_KEY')
# URL para la temporada actual (puedes cambiar el año según corresponda, ej. 2026)
URL = "https://v3.football.api-sports.io/fixtures?team=728&season=2026"

headers = {
    'x-apisports-key': API_KEY
}

req = urllib.request.Request(URL, headers=headers)

try:
    with urllib.request.urlopen(req) as response:
        raw_data = json.loads(response.read().decode())
        
        # Procesamos la respuesta de API-Football para limpiarla y dejarla lista para la app
        fixtures = raw_data.get("response", [])
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
            
            # Mapeamos los estados de la API a los que usa tu app (FINISHED, UPCOMING, etc.)
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
                "matchUrl": f"https://www.rayovallecano.es/partidos"
            }
            formatted_matches.append(match_obj)

        final_output = {
            "temporada": "2026/2027",
            "equipo_principal": "Rayo Vallecano",
            "partidos": formatted_matches
        }

        # Guardamos el resultado adaptado en matches.json
        with open("matches.json", "w", encoding="utf-8") as f:
            json.dump(final_output, f, indent=4, ensure_ascii=False)
            
        print(f"¡Éxito! Se han guardado {len(formatted_matches)} partidos en matches.json.")
except Exception as e:
    print(f"Error al conectar con la API: {e}")
    exit(1)
