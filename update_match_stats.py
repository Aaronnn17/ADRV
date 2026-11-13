from playwright.sync_api import sync_playwright
import json
import os
import re

def extraer_match_center_siguiente():
    datos_partido = {
        "match_center_url": "",
        "cara_a_cara": { "victorias_local": "0", "empates": "0", "victorias_visitante": "0" },
        "enfrentamientos_recientes": [],
        "racha_ultimos_partidos": [],
        "estadisticas_temporada": [],
        "estadisticas_jugadores": []
    }
    
    with open("match_stats.json", "w", encoding="utf-8") as f:
        json.dump(datos_partido, f, ensure_ascii=False, indent=4)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            viewport={"width": 1440, "height": 900}
        )
        page = context.new_page()
        
        print("Buscando el enlace del partido SIGUIENTE...")
        page.goto("https://www.rayovallecano.es/partidos", wait_until="domcontentloaded", timeout=30000)
        page.wait_for_timeout(4000)
        page.evaluate("window.scrollBy(0, 800)")
        page.wait_for_timeout(2000)

        url_partido = page.evaluate("""() => {
            const blocks = Array.from(document.querySelectorAll('div[class*="MkFootballModuleListMatches__matchList"] > div'));
            for (let block of blocks) {
                const title = block.querySelector('[class*="featuredTitle"]');
                if (title && title.innerText.toLowerCase().includes('siguiente')) {
                    const matchLink = block.querySelector('a[href*="/partidos/"]');
                    if (matchLink) return matchLink.href;
                }
            }
            return null;
        }""")

        if not url_partido:
            print("Error: No se encontró enlace al Match Center del Siguiente partido.")
            browser.close()
            return
            
        print(f"URL encontrada: {url_partido}")
        page.goto(url_partido, wait_until="domcontentloaded", timeout=30000)
        
        page.evaluate("""async () => {
            await new Promise((resolve) => {
                let totalHeight = 0; let distance = 400;
                let timer = setInterval(() => {
                    window.scrollBy(0, distance);
                    totalHeight += distance;
                    if (totalHeight >= document.body.scrollHeight) { clearInterval(timer); resolve(); }
                }, 150);
            });
        }""")
        page.wait_for_timeout(4000)

        # Usamos TreeWalker para extraer cada texto 100% aislado (sin apelotonamientos de React)
        extracted = page.evaluate("""() => {
            const getLeafTexts = (selector) => {
                const el = document.querySelector(selector);
                if (!el) return [];
                const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);
                const texts = []; let node;
                while (node = walker.nextNode()) {
                    let val = node.nodeValue.trim();
                    if (val && !['Cara a cara', 'Enfrentamientos recientes', 'Racha en los últimos partidos', 'Estadísticas de la temporada', 'Estadísticas de los jugadores'].includes(val)) {
                        texts.push(val);
                    }
                }
                return texts;
            };
            return {
                h2h: getLeafTexts('.MkSportViewHeadToHead'),
                recent: getLeafTexts('.MkSportViewRecentMeetings'),
                trend: getLeafTexts('.MkSportViewTeamTrend'),
                season: getLeafTexts('.MkSportViewHeadToHeadSeasonStats'),
                players: getLeafTexts('.MkSportPlayerComparison')
            };
        }""")

        datos_partido["match_center_url"] = url_partido

        # 1. ORDENAR CARA A CARA
        h2h_list = extracted.get("h2h", [])
        vic = []
        emp = "0"
        for i, t in enumerate(h2h_list):
            if "victorias" in t.lower() and i > 0 and h2h_list[i-1].isdigit():
                vic.append(h2h_list[i-1])
            if "empates" in t.lower() and i > 0 and h2h_list[i-1].isdigit():
                emp = h2h_list[i-1]
        
        if len(vic) >= 2:
            datos_partido["cara_a_cara"]["victorias_local"] = vic[0]
            datos_partido["cara_a_cara"]["victorias_visitante"] = vic[1]
        datos_partido["cara_a_cara"]["empates"] = emp

        # 2. ORDENAR ENFRENTAMIENTOS RECIENTES
        recent_clean = []
        chunk = []
        for t in extracted.get("recent", []):
            # Detectamos la fecha (ej: "14 may 2026") para saber cuándo empieza un partido nuevo
            if re.match(r'\d{1,2}\s+[a-zñ]{3,5}\s+\d{4}', t, re.IGNORECASE):
                if len(chunk) >= 4:
                    recent_clean.append({
                        "fecha": chunk[0], "local": chunk[1], "resultado": chunk[2], "visitante": chunk[3],
                        "competicion": chunk[4] if len(chunk) > 4 else ""
                    })
                chunk = [t]
            else:
                chunk.append(t)
        if len(chunk) >= 4:
            recent_clean.append({
                "fecha": chunk[0], "local": chunk[1], "resultado": chunk[2], "visitante": chunk[3],
                "competicion": chunk[4] if len(chunk) > 4 else ""
            })
        datos_partido["enfrentamientos_recientes"] = recent_clean[:5]

        # 3. ORDENAR RACHA ÚLTIMOS PARTIDOS
        racha_clean = []
        chunk = []
        for t in extracted.get("trend", []):
            if re.match(r'\d{1,2}/\d{1,2}/\d{2,4}', t):
                if len(chunk) >= 4:
                    racha_clean.append({
                        "fecha": chunk[0], "local": chunk[1], "resultado": chunk[2],
                        "visitante": chunk[3], "estado": chunk[4] if len(chunk)>4 else ""
                    })
                chunk = [t]
            else:
                # Limpiamos los textos sucios de la racha de la web
                if t == 'Empatee': t = 'Empate'
                elif t == 'Derrotad': t = 'Derrota'
                elif t == 'Victoriav': t = 'Victoria'
                chunk.append(t)
        if len(chunk) >= 4:
            racha_clean.append({
                "fecha": chunk[0], "local": chunk[1], "resultado": chunk[2],
                "visitante": chunk[3], "estado": chunk[4] if len(chunk)>4 else ""
            })
        datos_partido["racha_ultimos_partidos"] = racha_clean

        # 4. ORDENAR ESTADÍSTICAS TEMPORADA
        season_clean = []
        s_texts = extracted.get("season", [])
        i = 0
        while i < len(s_texts):
            if i + 2 < len(s_texts):
                v1, v2 = s_texts[i+1], s_texts[i+2]
                if re.match(r'^[\d,.]+[%]?$', v1) and re.match(r'^[\d,.]+[%]?$', v2):
                    season_clean.append({"estadistica": s_texts[i], "local": v1, "visitante": v2})
                    i += 3
                    continue
            i += 1
        datos_partido["estadisticas_temporada"] = season_clean

        # 5. ORDENAR ESTADÍSTICAS JUGADORES
        players_clean = []
        p_texts = extracted.get("players", [])
        i = 0
        while i < len(p_texts):
            if re.match(r'^[\d,.]+[%]?$', p_texts[i]):
                if i >= 2 and i + 2 < len(p_texts) and re.match(r'^[\d,.]+[%]?$', p_texts[i+1]):
                    players_clean.append({
                        "estadistica": p_texts[i-2],
                        "jugador_local": p_texts[i-1], "valor_local": p_texts[i],
                        "valor_visitante": p_texts[i+1], "jugador_visitante": p_texts[i+2]
                    })
                    i += 3
                    continue
            i += 1
        datos_partido["estadisticas_jugadores"] = players_clean

        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json completado con datos perfectamente estructurados!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_siguiente()
