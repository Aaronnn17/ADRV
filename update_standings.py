import json
import os
import requests

API_KEY = os.environ.get('FOOTBALL_API_KEY')
headers = {'X-Auth-Token': API_KEY} if API_KEY else {}

# Código 2014 es el ID de la Primera División Española (LaLiga)
standings_url = "https://api.football-data.org/v4/competitions/2014/standings"
querystring = {"season": "2026"}

try:
    response = requests.get(standings_url, headers=headers, params=querystring, timeout=15)
    
    if response.status_code == 200:
        data = response.json()
        
        # En la respuesta, 'standings' es una lista. 
        # Normalmente el primer elemento es la clasificación general ("TOTAL").
        standings = data.get("standings", [])
        general_table = []
        
        if standings:
            for standing_type in standings:
                if standing_type.get("type") == "TOTAL":
                    general_table = standing_type.get("table", [])
                    break
        
        formatted_standings = []
        for team_row in general_table:
            team_info = team_row.get("team", {})
            formatted_standings.append({
                "position": team_row.get("position"),
                "teamName": team_info.get("name"),
                "shortName": team_info.get("shortName"),
                "crest": team_info.get("crest"),
                "playedGames": team_row.get("playedGames"),
                "won": team_row.get("won"),
                "draw": team_row.get("draw"),
                "lost": team_row.get("lost"),
                "points": team_row.get("points"),
                "goalsFor": team_row.get("goalsFor"),
                "goalsAgainst": team_row.get("goalsAgainst"),
                "goalDifference": team_row.get("goalDifference")
            })

        final_output = {
            "metadata": {
                "competition": data.get("competition", {}).get("name"),
                "season": "2026/2027",
                "matchday": data.get("season", {}).get("currentMatchday")
            },
            "standings": formatted_standings
        }

        with open("standings.json", "w", encoding="utf-8") as f:
            json.dump(final_output, f, indent=4, ensure_ascii=False)
            
        print("standings.json creado con éxito.")

    else:
        print(f"Error al obtener clasificación. Code: {response.status_code}")

except Exception as e:
    print(f"Error en la ejecución: {e}")
