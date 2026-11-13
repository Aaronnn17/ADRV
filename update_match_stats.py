from playwright.sync_api import sync_playwright
import json
import os
import re
import urllib.parse

def limpiar_url_foto(photo_url):
    if not photo_url:
        return ""
    if "," in photo_url or " " in photo_url:
        photo_url = photo_url.split(",")[0].strip().split(" ")[0]
    if "/_next/image?url=" in photo_url:
        parsed_url = urllib.parse.urlparse(photo_url)
        qs = urllib.parse.parse_qs(parsed_url.query)
        if "url" in qs:
            photo_url = qs["url"][0]
    if photo_url.startswith("data:"):
        return ""
    elif photo_url.startswith("/"):
        photo_url = f"https://www.rayovallecano.es{photo_url}"
    return photo_url

def extraer_match_center_siguiente():
    datos_partido = {
        "match_center_url": "",
        "cara_a_cara": { "victorias_local": "0", "empates": "0", "victorias_visitante": "0" },
        "enfrentamientos_recientes": [],
        "racha_ultimos_partidos": {
            "local": [],
            "visitante": []
        },
        "estadisticas_temporada": [],
        "estadisticas_jugadores": []
    }
    
    with open("match_stats.json", "w", encoding="utf-8") as f:
        json.dump(datos_partido, f, ensure_ascii=False, indent=4)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
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
        page.wait_for_timeout(3000)
        
        # Scroll progresivo en 2 pasadas para asegurar carga de fotos e indicadores finales
        for _ in range(2):
            page.evaluate("""async () => {
                await new Promise((resolve) => {
                    let totalHeight = 0; let distance = 300;
                    let timer = setInterval(() => {
                        window.scrollBy(0, distance);
                        totalHeight += distance;
                        if (totalHeight >= document.body.scrollHeight + 1000) { clearInterval(timer); resolve(); }
                    }, 200);
                });
            }""")
            page.wait_for_timeout(2500)

        extracted = page.evaluate("""() => {
            const getLeafTexts = (selector) => {
                const els = document.querySelectorAll(selector);
                for (let el of els) {
                    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);
                    const texts = []; let node;
                    while (node = walker.nextNode()) {
                        let val = node.nodeValue.trim();
                        if (val && !['Cara a cara', 'Enfrentamientos recientes', 'Racha en los últimos partidos', 'Estadísticas de la temporada', 'Estadísticas de los jugadores'].includes(val)) {
                            texts.push(val);
                        }
                    }
                    if (texts.length > 0) return texts;
                }
                return [];
            };

            // Buscamos en TODOS los contenedores de comparación de jugadores (para saltar los que están ocultos con display: none)
            const getPlayerStats = () => {
                const containers = document.querySelectorAll('[class*="MatchPlayerComparison"]');
                const results = [];
                containers.forEach(container => {
                    const cards = container.querySelectorAll('[class*="StatBarComparison"]');
                    cards.forEach(card => {
                        const headerEl = card.querySelector('[class*="header"]');
                        if (!headerEl) return; // Solo procesamos las tarjetas principales

                        const walker = document.createTreeWalker(card, NodeFilter.SHOW_TEXT, null, false);
                        const texts = []; let node;
                        while (node = walker.nextNode()) {
                            let val = node.nodeValue.trim();
                            if (val && val !== 'Estadísticas de los jugadores') texts.push(val);
                        }
                        const imgs = Array.from(card.querySelectorAll('img')).map(img => img.currentSrc || img.src || img.getAttribute('srcset') || '');
                        if (texts.length >= 3) {
                            results.push({ texts: texts, imgs: imgs });
                        }
                    });
                });
                return results;
            };

            return {
                h2h: getLeafTexts('[class*="MatchHeadToHeadDoughnut"], .MkSportViewHeadToHead'),
                recent: getLeafTexts('[class*="MatchHeadToHeadMeetings"], .MkSportViewRecentMeetings'),
                trend: getLeafTexts('[class*="MatchHeadToHeadTeamTrend"], .MkSportViewTeamTrend'),
                season: getLeafTexts('[class*="MatchHeadToHeadSeasonStats"]'),
                players: getPlayerStats()
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
            if re.match(r'^\d{1,2}\s+[a-zñ]{3,5}\s+\d{4}$', t, re.IGNORECASE):
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

        # 3. ORDENAR RACHA ÚLTIMOS PARTIDOS (Corregido estado[0] -> V, E, D)
        racha_all = []
        chunk = []
        
        def obtener_inicial_estado(raw_str):
            s = raw_str.strip().upper()
            if s.startswith("V"): return "V"
            if s.startswith("E"): return "E"
            if s.startswith("D"): return "D"
            return s[0] if s else ""

        def procesar_chunk_racha(c):
            if len(c) >= 6 and c[3] == "-":
                estado_raw = c[6] if len(c) > 6 else ""
                return {
                    "fecha": c[0],
                    "local": c[1],
                    "resultado": f"{c[2]} - {c[4]}",
                    "visitante": c[5],
                    "estado": obtener_inicial_estado(estado_raw)
                }
            elif len(c) >= 4:
                estado_raw = c[4] if len(c) > 4 else ""
                return {
                    "fecha": c[0],
                    "local": c[1],
                    "resultado": c[2],
                    "visitante": c[3],
                    "estado": obtener_inicial_estado(estado_raw)
                }
            return None

        for t in extracted.get("trend", []):
            if "datos de:" in t.lower():
                continue
            if re.match(r'^\d{1,2}/\d{1,2}/\d{2,4}$', t):
                item = procesar_chunk_racha(chunk)
                if item: racha_all.append(item)
                chunk = [t]
            else:
                chunk.append(t)
        item = procesar_chunk_racha(chunk)
        if item: racha_all.append(item)

        datos_partido["racha_ultimos_partidos"] = {
            "local": racha_all[:5],
            "visitante": racha_all[5:10]
        }

        # 4. ORDENAR ESTADÍSTICAS DE LA TEMPORADA
        s_raw = [t for t in extracted.get("season", []) if t.lower() != "por partido"]
        if len(s_raw) >= 2 and not re.match(r'^[\d,.]+[%]?$', s_raw[1]):
            s_raw = s_raw[2:]

        season_clean = []
        i = 0
        is_num = lambda x: bool(re.match(r'^[\d,.]+[%]?$', x))

        while i < len(s_raw):
            if not is_num(s_raw[i]):
                titulo = s_raw[i]
                nums = []
                j = i + 1
                while j < len(s_raw) and is_num(s_raw[j]):
                    nums.append(s_raw[j])
                    j += 1
                if len(nums) == 2:
                    season_clean.append({
                        "estadistica": titulo,
                        "local": nums[0],
                        "visitante": nums[1]
                    })
                elif len(nums) >= 4:
                    season_clean.append({
                        "estadistica": titulo,
                        "media_local": nums[0],
                        "local": nums[1],
                        "visitante": nums[2],
                        "media_visitante": nums[3]
                    })
                i = j
            else:
                i += 1
        datos_partido["estadisticas_temporada"] = season_clean

        # 5. ORDENAR ESTADÍSTICAS DE JUGADORES (Sin duplicados y limpiando URLs de Next.js)
        players_clean = []
        vistas = set()
        for card in extracted.get("players", []):
            t_list = card.get("texts", [])
            imgs = card.get("imgs", [])
            if not t_list: continue
            
            titulo = t_list[0]
            if titulo in vistas:
                continue
                
            nums = [x for x in t_list[1:] if is_num(x)]
            names = [x for x in t_list[1:] if not is_num(x)]
            
            if len(nums) >= 2 and len(names) >= 2:
                vistas.add(titulo)
                players_clean.append({
                    "estadistica": titulo,
                    "jugador_local": names[0],
                    "valor_local": nums[0],
                    "foto_local": limpiar_url_foto(imgs[0]) if len(imgs) > 0 else "",
                    "jugador_visitante": names[1],
                    "valor_visitante": nums[1],
                    "foto_visitante": limpiar_url_foto(imgs[1]) if len(imgs) > 1 else ""
                })
        datos_partido["estadisticas_jugadores"] = players_clean

        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json completado al 100%!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_siguiente()
