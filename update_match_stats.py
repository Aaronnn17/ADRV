from playwright.sync_api import sync_playwright
import json
import os
from datetime import datetime, timezone

def extraer_match_center_robusto():
    # 1. CREAMOS EL SALVAVIDAS
    datos_partido = {
        "match_center_url": "",
        "cara_a_cara": {},
        "enfrentamientos_recientes": [],
        "racha_ultimos_partidos": [],
        "estadisticas_temporada": [],
        "estadisticas_jugadores": []
    }
    with open("match_stats.json", "w", encoding="utf-8") as f:
        json.dump(datos_partido, f, ensure_ascii=False, indent=4)

    # 2. OBTENEMOS LA URL DEL PARTIDO DESDE matches.json
    if not os.path.exists("matches.json"):
        print("Error: matches.json no existe. Cancelando.")
        return

    with open("matches.json", "r", encoding="utf-8") as f:
        data_matches = json.load(f)

    partidos = data_matches.get("partidos", [])
    if not partidos:
        return

    ahora = datetime.now(timezone.utc)
    url_partido = None
    home = ""
    away = ""

    for match in partidos:
        try:
            date_text = match.get("dateText", "")
            match_date = datetime.fromisoformat(date_text.replace("Z", "+00:00"))
            if match_date >= ahora:
                # Extraemos la url o construimos la base
                url_partido = match.get("matchCenterUrl", "")
                home = match.get("homeTeam", "").lower().replace(" ", "-")
                away = match.get("awayTeam", "").lower().replace(" ", "-")
                break
        except Exception:
            continue

    # Si no hay URL directa en tu JSON, la construimos por defecto
    if not url_partido and home and away:
        # Esto asume el formato estándar de la web del rayo. Si puedes añadir matchCenterUrl en tu script de calendario, mejor.
        print("Aviso: Construyendo URL de forma manual porque no viene en el JSON.")
        url_partido = f"https://www.rayovallecano.es/partidos/temporada-2024-2025-laliga-ea-sports-13-{home}-vs-{away}-0000"

    if not url_partido:
        print("Error: No hay próximo partido.")
        return

    print(f"Navegando directamente a la URL del Match Center: {url_partido}")

    # 3. EXTRACCIÓN AL ESTILO SQUAD.PY (Con JS Inyectado)
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            viewport={"width": 1440, "height": 900}
        )
        page = context.new_page()
        
        # Entramos directamente a la URL del partido
        page.goto(url_partido, wait_until="domcontentloaded", timeout=30000)
        page.wait_for_timeout(3000)

        # Hacemos scroll progresivo (Igual que en squad.py)
        page.evaluate("""async () => {
            await new Promise((resolve) => {
                let totalHeight = 0;
                let distance = 350;
                let timer = setInterval(() => {
                    let scrollHeight = document.body.scrollHeight;
                    window.scrollBy(0, distance);
                    totalHeight += distance;
                    if (totalHeight >= scrollHeight) {
                        clearInterval(timer);
                        resolve();
                    }
                }, 150);
            });
        }""")
        page.wait_for_timeout(3000)

        # Inyectamos JS para extraer los bloques de estadísticas (Igual que extrajimos las cards en squad)
        extracted_data = page.evaluate("""() => {
            const getTexts = (selector) => {
                const el = document.querySelector(selector);
                if (!el) return [];
                const items = el.querySelectorAll('div');
                let texts = [];
                items.forEach(i => {
                    const t = i.innerText.trim();
                    if(t) texts.push(t);
                });
                return texts;
            };

            return {
                h2h: getTexts('.MkSportViewHeadToHead'),
                recent: getTexts('.MkSportViewRecentMeetings'),
                trend: getTexts('.MkSportViewTeamTrend'),
                season: getTexts('.MkSportViewHeadToHeadSeasonStats'),
                players: getTexts('.MkSportPlayerComparison')
            };
        }""")

        # 4. VOLCAMOS LOS DATOS AL JSON SALVAVIDAS
        datos_partido["match_center_url"] = url_partido
        datos_partido["cara_a_cara"]["resumen"] = extracted_data.get("h2h", [])[:15]
        datos_partido["enfrentamientos_recientes"] = extracted_data.get("recent", [])[:15]
        datos_partido["racha_ultimos_partidos"] = extracted_data.get("trend", [])[:15]
        datos_partido["estadisticas_temporada"] = extracted_data.get("season", [])[:30]
        datos_partido["estadisticas_jugadores"] = extracted_data.get("players", [])[:20]

        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json completado con éxito mediante page.evaluate!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_robusto()
