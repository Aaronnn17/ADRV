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
            
            score = match.get("score", {})
            full_time = score.get("fullTime", {})
            half_time = score.get("halfTime", {})
            
            referees = match.get("referees", [])
            main_referee = referees[0].get("name", "Por designar") if referees else "Por designar"
            
            status_api = match.get("status", "SCHEDULED")
            competition_name = match.get("competition", {}).get("name", "LaLiga")

            # --- NUEVO: Intentamos extraer goles y tarjetas (eventos) si la API los manda ---
            match_events = []
            
            # Algunos endpoints devuelven 'goals' (una lista)
            if "goals" in match:
                for goal in match["goals"]:
                    scorer = goal.get("scorer", {}).get("name", "Desconocido")
                    minute = goal.get("minute", "")
                    team_name = goal.get("team", {}).get("name", "")
                    match_events.append({
                        "type": "GOAL",
                        "minute": str(minute),
                        "player": scorer,
                        "team": team_name
                    })
                    
            # Algunos endpoints devuelven 'bookings' (tarjetas)
            if "bookings" in match:
                for booking in match["bookings"]:
                    card_type = booking.get("card", "YELLOW_CARD")
                    player = booking.get("player", {}).get("name", "Desconocido")
                    minute = booking.get("minute", "")
                    team_name = booking.get("team", {}).get("name", "")
                    
                    event_type = "RED_CARD" if "RED" in card_type else "YELLOW_CARD"
                    match_events.append({
                        "type": event_type,
                        "minute": str(minute),
                        "player": player,
                        "team": team_name
                    })
            
            # Ordenamos los eventos por minuto (si se puede convertir a número)
            try:
                match_events.sort(key=lambda x: int(x["minute"]) if x["minute"].isdigit() else 999)
            except:
                pass


            match_obj = {
                "id": f"match_{match.get('id', '')}",
                "homeTeam": home_team,
                "awayTeam": away_team,
                "homeScore": full_time.get("home"),
                "awayScore": full_time.get("away"),
                "halfTimeHomeScore": half_time.get("home"),
                "halfTimeAwayScore": half_time.get("away"),
                "status": "FINISHED" if status_api == "FINISHED" else "UPCOMING",
                "statusApi": status_api, 
                "dateText": match.get("utcDate", "Próximo partido"),
                "competition": competition_name,
                "round": f"Jornada {match.get('matchday', '')}",
                "homeLogoUrl": match.get("homeTeam", {}).get("crest", ""),
                "awayLogoUrl": match.get("awayTeam", {}).get("crest", ""),
                "venue": team_info.get("venue", "Estadio de Vallecas") if "Rayo" in home_team else "Estadio Visitante",
                "referee": main_referee,
                "events": match_events # NUEVO: Añadimos la lista de eventos
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
