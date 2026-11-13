from playwright.sync_api import sync_playwright
import json
import os

def extraer_match_center_siguiente():
    # 1. PLANTILLA BASE
    datos_partido = {
        "match_center_url": "",
        "cara_a_cara": {
            "victorias_local": "",
            "empates": "",
            "victorias_visitante": ""
        },
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

    print("Iniciando extracción del partido SIGUIENTE...")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            viewport={"width": 1440, "height": 900}
        )
        page = context.new_page()
        
        # 2. ENCONTRAR LA URL DEL PARTIDO "SIGUIENTE"
        page.goto("https://www.rayovallecano.es/partidos", wait_until="domcontentloaded", timeout=30000)
        page.wait_for_timeout(4000)
        
        # Hacemos un poco de scroll para que cargue la lista
        page.evaluate("window.scrollBy(0, 800)")
        page.wait_for_timeout(2000)

        # Buscamos el contenedor que tiene el texto "Siguiente" y sacamos su enlace al Match Center
        url_partido = page.evaluate("""() => {
            // Buscamos todos los bloques de partido destacados
            const featuredBlocks = Array.from(document.querySelectorAll('div[class*="MkFootballModuleListMatches__matchList"] > div'));
            
            for (let block of featuredBlocks) {
                const title = block.querySelector('[class*="featuredTitle"]');
                if (title && title.innerText.toLowerCase().includes('siguiente')) {
                    // Si este bloque es el "Siguiente", buscamos su enlace de Match Center
                    const matchLink = block.querySelector('a[href*="/partidos/"]');
                    if (matchLink) return matchLink.href;
                }
            }
            return null;
        }""")

        if not url_partido:
            print("Error: No se encontró el enlace dentro del bloque 'Siguiente'.")
            browser.close()
            return
            
        print(f"URL del SIGUIENTE partido encontrada: {url_partido}")
        datos_partido["match_center_url"] = url_partido

        # 3. EXTRAER LAS ESTADÍSTICAS LIMPIAS EN EL MATCH CENTER
        page.goto(url_partido, wait_until="domcontentloaded", timeout=30000)
        
        # Scroll para cargar todos los gráficos
        page.evaluate("""async () => {
            await new Promise((resolve) => {
                let totalHeight = 0;
                let distance = 400;
                let timer = setInterval(() => {
                    let scrollHeight = document.body.scrollHeight;
                    window.scrollBy(0, distance);
                    totalHeight += distance;
                    if (totalHeight >= scrollHeight) {
                        clearInterval(timer);
                        resolve();
                    }
                }, 200);
            });
        }""")
        page.wait_for_timeout(4000)

        # Inyectamos JS para estructurar los datos limpiamente
        extracted_data = page.evaluate("""() => {
            const data = {
                h2h: { victorias_local: "", empates: "", victorias_visitante: "" },
                recent: [],
                trend: { local: [], visitante: [] },
                season: [],
                players: []
            };

            // 1. CARA A CARA (Buscamos los números grandes de victorias/empates)
            const h2hContainer = document.querySelector('.MkSportViewHeadToHead');
            if (h2hContainer) {
                // Buscamos los textos que contengan solo números (asumiendo orden: Local, Empates, Visitante)
                const circles = Array.from(h2hContainer.querySelectorAll('div'))
                                    .filter(d => /^\\d+$/.test(d.innerText.trim()));
                if (circles.length >= 3) {
                    data.h2h.victorias_local = circles[0].innerText.trim();
                    data.h2h.victorias_visitante = circles[1].innerText.trim(); // El 2º suele ser el visitante
                    data.h2h.empates = circles[2].innerText.trim(); // El de abajo suele ser empates
                }
            }

            // 2. ENFRENTAMIENTOS RECIENTES
            const recentContainer = document.querySelector('.MkSportViewRecentMeetings');
            if (recentContainer) {
                const rows = recentContainer.querySelectorAll('article, div[class*="MatchHeadToHeadMeetings"] > div');
                rows.forEach(row => {
                    const text = row.innerText.trim();
                    // Limpieza básica del texto apelotonado (puedes mejorar esta regex si es necesario)
                    if (text.length > 10 && !data.recent.includes(text)) {
                        data.recent.push(text.replace(/\\n/g, ' - '));
                    }
                });
            }

            // 3. ESTADÍSTICAS DE TEMPORADA (Barras de comparación)
            const seasonContainer = document.querySelector('.MkSportViewHeadToHeadSeasonStats');
            if (seasonContainer) {
                const bars = seasonContainer.querySelectorAll('.MkSportViewStatBarComparison');
                bars.forEach(bar => {
                    const header = bar.querySelector('[class*="header"]');
                    const values = bar.querySelectorAll('[class*="value"], [class*="Player"]'); // Ajustar selectores según DOM
                    
                    if (header) {
                        const title = header.innerText.trim();
                        // Sacamos todos los textos sueltos de la barra
                        const allTexts = Array.from(bar.querySelectorAll('div')).map(d => d.innerText.trim()).filter(t => t);
                        // Filtramos para coger solo los números o porcentajes
                        const numbers = allTexts.filter(t => /^\\d+(,\\d+)?%?$/.test(t));
                        
                        if (numbers.length >= 2) {
                            data.season.push({
                                stat: title,
                                home: numbers[0],
                                away: numbers[1]
                            });
                        }
                    }
                });
            }

            return data;
        }""")

        # 4. ACTUALIZAR EL JSON
        if extracted_data:
            datos_partido["cara_a_cara"] = extracted_data.get("h2h", datos_partido["cara_a_cara"])
            datos_partido["enfrentamientos_recientes"] = extracted_data.get("recent", [])[:5]
            datos_partido["estadisticas_temporada"] = extracted_data.get("season", [])

        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json completado con datos estructurados!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_siguiente()
