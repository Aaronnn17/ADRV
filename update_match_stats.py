from playwright.sync_api import sync_playwright
import json
import os
from datetime import datetime, timezone

def extraer_match_center_desde_json():
    # 1. Leemos el archivo matches.json que ya genera tu script de calendario
    if not os.path.exists("matches.json"):
        print("Error: No se encuentra el archivo matches.json en el repositorio.")
        return

    with open("matches.json", "r", encoding="utf-8") as f:
        data_matches = json.load(f)

    # Buscamos la lista de partidos
    partidos = data_matches.get("partidos", [])
    if not partidos:
        print("No hay partidos en matches.json")
        return

    # Buscamos el próximo partido (el primero que no sea anterior a hoy)
    ahora = datetime.now(timezone.utc)
    url_match_center = None

    for match in partidos:
        try:
            date_text = match.get("dateText", "")
            match_date = datetime.fromisoformat(date_text.replace("Z", "+00:00"))
            if match_date >= ahora:
                # Intentamos construir la URL del match center o sacarla si viene incluida
                # Si tu JSON de partidos guarda el ID o enlace, lo usamos. 
                # Si no, construimos la ruta estándar basada en los equipos y la competición de la web del Rayo:
                home = match.get("homeTeam", "").lower().replace(" ", "-")
                away = match.get("awayTeam", "").lower().replace(" ", "-")
                
                # Como alternativa robusta, buscamos si el objeto match trae algún campo de enlace directo
                url_match_center = match.get("matchCenterUrl", "")
                break
        except Exception:
            continue

    # Si el json no trae la URL directa, abrimos la web de partidos para capturarla con seguridad
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        print("Entrando a la sección de partidos del Rayo para localizar el Match Center...")
        page.goto("https://www.rayovallecano.es/partidos")
        page.wait_for_timeout(5000)
        
        # Hacemos scroll hacia abajo para forzar la carga de los elementos de la lista
        page.evaluate("window.scrollBy(0, 800)")
        page.wait_for_timeout(3000)

        # Buscamos de forma precisa cualquier enlace que contenga la palabra "partidos" y sea un match center
        enlaces = page.locator("a[href*='/partidos/']").all()
        url_partido = None
        
        for enlace in enlaces:
            href = enlace.get_attribute("href")
            texto = enlace.inner_text().lower()
            if href and ("match center" in texto or "temporada" in href):
                if href.startswith("/"):
                    url_partido = "https://www.rayovallecano.es" + href
                else:
                    url_partido = href
                break

        if not url_partido:
            print("Error: No se pudo encontrar ningún enlace al Match Center en la página.")
            browser.close()
            return

        print(f"¡Match Center encontrado! Navegando a: {url_partido}")
        page.goto(url_partido)
        page.wait_for_timeout(5000) # Esperamos que carguen todas las pestañas de estadísticas

        datos_partido = {
            "match_center_url": url_partido,
            "cara_a_cara": {},
            "enfrentamientos_recientes": [],
            "racha_ultimos_partidos": [],
            "estadisticas_temporada": [],
            "estadisticas_jugadores": []
        }

        # --- EXTRACCIÓN DE LAS 5 SECCIONES ---
        try:
            h2h = page.locator(".MkSportViewHeadToHead")
            if h2h.count() > 0:
                datos_partido["cara_a_cara"]["resumen"] = [t.strip() for t in h2h.locator("div").all_inner_texts() if t.strip()][:15]
        except Exception:
            pass

        try:
            recent = page.locator(".MkSportViewRecentMeetings")
            if recent.count() > 0:
                datos_partido["enfrentamientos_recientes"] = [p.strip() for p in recent.locator("div").all_inner_texts() if p.strip()][:15]
        except Exception:
            pass

        try:
            trend = page.locator(".MkSportViewTeamTrend")
            if trend.count() > 0:
                datos_partido["racha_ultimos_partidos"] = [t.strip() for t in trend.locator("div").all_inner_texts() if t.strip()][:15]
        except Exception:
            pass

        try:
            stats = page.locator(".MkSportViewHeadToHeadSeasonStats")
            if stats.count() > 0:
                datos_partido["estadisticas_temporada"] = [s.strip() for s in stats.locator("div").all_inner_texts() if s.strip()][:30]
        except Exception:
            pass

        try:
            players = page.locator(".MkSportPlayerComparison")
            if players.count() > 0:
                datos_partido["estadisticas_jugadores"] = [pt.strip() for pt in players.locator("div").all_inner_texts() if pt.strip()][:20]
        except Exception:
            pass

        # Guardamos el JSON final que subirá el workflow
        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json generado con éxito!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_desde_json()
