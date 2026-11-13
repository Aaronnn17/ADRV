from playwright.sync_api import sync_playwright
import json
import os

def extraer_match_center_completo():
    with sync_playwright() as p:
        # Lanzamos el navegador en segundo plano (headless)
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        # 1. Entramos a la web principal de partidos del Rayo
        print("Entrando a la web del Rayo Vallecano...")
        page.goto("https://www.rayovallecano.es/partidos")
        
        # Esperamos a que cargue y hacemos un pequeño scroll para asegurar que se renderiza el "Siguiente" partido
        page.wait_for_timeout(4000)
        page.evaluate("window.scrollTo(0, 500)")
        page.wait_for_timeout(2000)

        # 2. Búsqueda flexible del botón o enlace del Match Center del próximo partido
        # Buscamos cualquier enlace que tenga texto parecido a "Match center" (ignorando mayúsculas)
        match_link_elem = page.locator("a:has-text('Match center'), a:has-text('Match Center'), [href*='/partidos/temporada']").first
        
        if not match_link_elem.is_visible():
            print("No se encontró el Match Center del próximo partido de forma directa.")
            # Intento alternativo: buscar por la clase o tarjeta del siguiente partido
            match_link_elem = page.locator("article").first.locator("a[href*='/partidos/']").first

        if not match_link_elem.is_visible():
            print("Error crítico: Imposible localizar el enlace del partido.")
            browser.close()
            return

        url_partido = match_link_elem.get_attribute("href")
        if url_partido.startswith("/"):
            url_partido = "https://www.rayovallecano.es" + url_partido

        print(f"¡Encontrado! Navegando al Match Center: {url_partido}")
        page.goto(url_partido)
        page.wait_for_timeout(4000)

        datos_partido = {
            "match_center_url": url_partido,
            "cara_a_cara": {},
            "enfrentamientos_recientes": [],
            "racha_ultimos_partidos": [],
            "estadisticas_temporada": [],
            "estadisticas_jugadores": []
        }

        # --- SECCIÓN 1: CARA A CARA ---
        try:
            h2h_box = page.locator(".MkSportViewHeadToHead")
            if h2h_box.count() > 0:
                textos = h2h_box.locator("div").all_inner_texts()
                datos_partido["cara_a_cara"]["resumen"] = [t.strip() for t in textos if t.strip()][:15]
        except Exception as e:
            print(f"Error en Cara a cara: {e}")

        # --- SECCIÓN 2: ENFRENTAMIENTOS RECIENTES ---
        try:
            recent_box = page.locator(".MkSportViewRecentMeetings")
            if recent_box.count() > 0:
                partidos_previos = recent_box.locator("article, div[class*='match']").all_inner_texts()
                datos_partido["enfrentamientos_recientes"] = [p.strip() for p in partidos_previos if p.strip()][:10]
        except Exception as e:
            print(f"Error en Enfrentamientos recientes: {e}")

        # --- SECCIÓN 3: RACHA EN LOS ÚLTIMOS PARTIDOS ---
        try:
            trend_box = page.locator(".MkSportViewTeamTrend")
            if trend_box.count() > 0:
                tendencias = trend_box.locator("div").all_inner_texts()
                datos_partido["racha_ultimos_partidos"] = [t.strip() for t in tendencias if t.strip()][:15]
        except Exception as e:
            print(f"Error en Racha: {e}")

        # --- SECCIÓN 4: ESTADÍSTICAS DE LA TEMPORADA ---
        try:
            stats_box = page.locator(".MkSportViewHeadToHeadSeasonStats")
            if stats_box.count() > 0:
                stats_textos = stats_box.locator("div").all_inner_texts()
                datos_partido["estadisticas_temporada"] = [s.strip() for s in stats_textos if s.strip()][:30]
        except Exception as e:
            print(f"Error en Estadísticas de temporada: {e}")

        # --- SECCIÓN 5: ESTADÍSTICAS DE JUGADORES ---
        try:
            player_box = page.locator(".MkSportPlayerComparison")
            if player_box.count() > 0:
                player_textos = player_box.locator("div").all_inner_texts()
                datos_partido["estadisticas_jugadores"] = [pt.strip() for pt in player_textos if pt.strip()][:20]
        except Exception as e:
            print(f"Error en Estadísticas de jugadores: {e}")

        # 3. Guardar el JSON final estructurado
        with open("match_stats.json", "w", encoding="utf-8") as f:
            json.dump(datos_partido, f, ensure_ascii=False, indent=4)

        print("¡Archivo match_stats.json generado correctamente con toda la información!")
        browser.close()

if __name__ == "__main__":
    extraer_match_center_completo()
