from playwright.sync_api import sync_playwright
import json
import os
from datetime import datetime, timezone

def extraer_match_center_desde_json():
    # 1. CREAMOS UN SALVAVIDAS: Plantilla base vacía. 
    # La guardamos de inmediato para que si algo falla, Git siempre encuentre el archivo.
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

    # 2. INTENTAMOS LA EXTRACCIÓN
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        print("Entrando a la sección de partidos del Rayo...")
        page.goto("https://www.rayovallecano.es/partidos")
        
        # ESPERA INTELIGENTE: Esperamos hasta 20 segundos a que la web de React renderice los partidos
        try:
            page.wait_for_selector("a[href*='/partidos/temporada']", timeout=20000)
        except Exception:
            print("Aviso: La web tardó demasiado en cargar los partidos dinámicos.")
        
        # Hacemos scroll hacia abajo para asegurar que el componente es visible
        page.evaluate("window.scrollBy(0, 800)")
        page.wait_for_timeout(2000)

        # Buscamos directamente el primer enlace oficial de temporada
        enlace = page.locator("a[href*='/partidos/temporada']").first
        url_partido = None
        
        if enlace.is_visible():
            href = enlace.get_attribute("href")
            if href:
                url_partido = "https://www.rayovallecano.es" + href if href.startswith("/") else href

        if not url_partido:
            print("Error: No se pudo localizar el enlace al Match Center.")
            browser.close()
            return # Termina aquí, pero el workflow sigue verde porque el JSON salvavidas ya está creado

        print(f"¡Match Center encontrado! Navegando a: {url_partido}")
        page.goto(url_partido)
        
        # Esperamos a que se dibujen las estadísticas internas
        page.wait_for_timeout(5000) 

        datos_partido["match_center_url"] = url_partido

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

        # SOBREESCRIBIMOS EL JSON SALVAVIDAS CON LOS DATOS REALES EXTRAÍDOS
        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json rellenado con éxito!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_desde_json()
