import json
import os
import urllib.request

API_KEY = os.environ.get('FOOTBALL_API_KEY')
URL = "https://v3.football.api-sports.io/fixtures?team=728&season=2026"

headers = {
    'x-apisports-key': API_KEY
}

req = urllib.request.Request(URL, headers=headers)

try:
    with urllib.request.urlopen(req) as response:
        raw_data = json.loads(response.read().decode())
        
        # Aquí puedes filtrar y adaptar la estructura a lo que tu app necesita exactamente
        # Guardamos el resultado adaptado en matches.json
        with open("matches.json", "w", encoding="utf-8") as f:
            json.dump(raw_data, f, indent=4, ensure_ascii=False)
            
        print("matches.json actualizado correctamente.")
except Exception as e:
    print(f"Error al actualizar los partidos: {e}")
    exit(1)
