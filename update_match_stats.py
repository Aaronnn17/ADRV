from playwright.sync_api import sync_playwright
import json
import os

def extraer_match_center_definitivo():
    # 1. CREAMOS EL SALVAVIDAS (JSON vacío por si acaso)
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

    print("Iniciando extracción robusta del Match Center...")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            viewport={"width": 1440, "height": 900}
        )
        page = context.new_page()
        
        # 2. ENCONTRAR LA URL REAL DEL PARTIDO
        print("Buscando el enlace del próximo partido en la web oficial...")
        page.goto("https://www.rayovallecano.es/partidos", wait_until="domcontentloaded", timeout=30000)
        
        # Hacemos scroll progresivo para asegurar que cargan los partidos de Next.js
        page.evaluate("""async () => {
            await new Promise((resolve) => {
                let totalHeight = 0;
                let distance = 350;
                let timer = setInterval(() => {
                    let scrollHeight = document.body.scrollHeight;
                    window.scrollBy(0, distance);
                    totalHeight += distance;
                    if (totalHeight >= 1500) { // Solo necesitamos bajar un poco para el próximo partido
                        clearInterval(timer);
                        resolve();
                    }
                }, 150);
            });
        }""")
        page.wait_for_timeout(2000)

        # Extraemos todos los enlaces de la página buscando el "Match center"
        url_partido = page.evaluate("""() => {
            let links = Array.from(document.querySelectorAll('a'));
            let matchLink = links.find(a => 
                (a.href.includes('/partidos/') && a.innerText.toLowerCase().includes('match center')) ||
                (a.href.includes('temporada-202') && a.href.includes('-vs-'))
            );
            return matchLink ? matchLink.href : null;
        }""")

        if not url_partido:
            print("Error crítico: No se encontró la URL real del Match Center en la web.")
            browser.close()
            return
            
        print(f"URL válida encontrada: {url_partido}")

        # 3. EXTRAER LAS ESTADÍSTICAS
        print("Navegando al Match Center para extraer datos...")
        page.goto(url_partido, wait_until="domcontentloaded", timeout=30000)
        
        # Scroll progresivo completo
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

        # Extracción vía DOM
        extracted_data = page.evaluate("""() => {
            const getTexts = (selector) => {
                const el = document.querySelector(selector);
                if (!el) return [];
                const items = el.querySelectorAll('div');
                let texts = [];
                items.forEach(i => {
                    const t = i.innerText.trim();
                    if(t && !texts.includes(t)) texts.push(t);
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

        # 4. VOLCAR LOS DATOS
        datos_partido["match_center_url"] = url_partido
        datos_partido["cara_a_cara"]["resumen"] = extracted_data.get("h2h", [])[:15]
        datos_partido["enfrentamientos_recientes"] = extracted_data.get("recent", [])[:15]
        datos_partido["racha_ultimos_partidos"] = extracted_data.get("trend", [])[:15]
        datos_partido["estadisticas_temporada"] = extracted_data.get("season", [])[:30]
        datos_partido["estadisticas_jugadores"] = extracted_data.get("players", [])[:20]

        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json completado con éxito!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_definitivo()
