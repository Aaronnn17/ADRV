"""
Obtiene las fotos de portada de Union Rayo con un navegador real (Playwright),
igual que update_squad.py hace con la web oficial del Rayo.

Guarda unionrayo_images.json ({"images": {url_noticia: url_imagen}}), el mismo formato
que ya lee update_news.py. Si la web muestra la verificacion anti-bot y no se resuelve,
muestra el motivo y sale con codigo 1 SIN tocar el fichero anterior.

v2: usa Google Chrome real (channel="chrome", ya instalado en los runners de GitHub),
oculta navigator.webdriver y espera hasta ~60 s a que se resuelva la verificacion,
mostrando cada cambio de titulo.

Dependencias: pip install playwright && playwright install --with-deps chromium
"""
import json
import sys
from datetime import datetime

from playwright.sync_api import sync_playwright

BASE = "https://unionrayo.es/"
OUT = "unionrayo_images.json"
MAX_ENTRIES = 300
WAIT_STEPS = 30          # 30 x 2 s = 60 s maximo esperando a la verificacion
BROWSER_ARGS = ["--no-sandbox", "--disable-blink-features=AutomationControlled"]

SCROLL_JS = """async () => {
  for (let i = 0; i < 40; i++) {
    window.scrollBy(0, 400);
    await new Promise(r => setTimeout(r, 150));
    if (window.innerHeight + window.scrollY >= document.body.scrollHeight) break;
  }
}"""

EXTRACT_JS = """() => {
  const out = [];
  document.querySelectorAll('.jeg_thumb').forEach(t => {
    const a = t.querySelector('a[href]');
    const img = t.querySelector('img');
    if (!a || !img) return;
    const srcset = img.getAttribute('data-lazy-srcset') || img.getAttribute('data-srcset') || img.getAttribute('srcset') || '';
    let best = '', bestW = -1;
    srcset.split(',').forEach(part => {
      const bits = part.trim().split(/\\s+/);
      if (!bits[0]) return;
      const w = (bits[1] && bits[1].endsWith('w')) ? parseInt(bits[1]) : 0;
      if (w > bestW) { best = bits[0]; bestW = w; }
    });
    const src = best || img.currentSrc || img.getAttribute('data-lazy-src') || img.getAttribute('data-src') || img.src || '';
    out.push({href: a.href, src: src});
  });
  return out;
}"""


def norm_link(url):
    return (url or "").split("#")[0].split("?")[0].rstrip("/")


def load_old():
    try:
        with open(OUT, encoding="utf-8") as f:
            return json.load(f).get("images", {})
    except Exception:
        return {}


def launch(p):
    try:
        browser = p.chromium.launch(channel="chrome", headless=True, args=BROWSER_ARGS)
        print("Navegador: Google Chrome (channel=chrome)")
        return browser
    except Exception as e:
        print(f"Chrome no disponible ({str(e)[:100]}). Uso Chromium de Playwright.")
        return p.chromium.launch(headless=True, args=BROWSER_ARGS)


def main():
    with sync_playwright() as p:
        browser = launch(p)
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            viewport={"width": 1440, "height": 900},
            locale="es-ES",
            timezone_id="Europe/Madrid",
        )
        context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});")
        page = context.new_page()

        try:
            resp = page.goto(BASE, wait_until="domcontentloaded", timeout=45000)
        except Exception as e:
            print(f"No se pudo cargar la home: {e}")
            browser.close()
            sys.exit(1)

        print(f"HTTP inicial {resp.status if resp else '?'}")

        # Espera a que se resuelva la verificacion anti-bot y aparezcan las tarjetas
        last_title = None
        found = False
        for i in range(WAIT_STEPS):
            try:
                title = page.title()
                n = page.locator(".jeg_thumb").count()
            except Exception:
                # la pagina esta navegando (la verificacion recarga la URL)
                page.wait_for_timeout(2000)
                continue
            if title != last_title:
                print(f"  [{i * 2:>2}s] titulo: {title!r} | url: {page.url}")
                last_title = title
            if n > 0:
                found = True
                break
            page.wait_for_timeout(2000)

        if not found:
            try:
                body = page.inner_text("body")[:400].replace("\n", " ")
            except Exception:
                body = "(sin body)"
            print(f"Sin tarjetas .jeg_thumb tras {WAIT_STEPS * 2} s. Texto de la pagina: {body}")
            browser.close()
            sys.exit(1)

        page.evaluate(SCROLL_JS)  # fuerza la carga de imagenes lazy
        page.wait_for_timeout(1500)
        items = page.evaluate(EXTRACT_JS)
        browser.close()

    images = {}
    for it in items:
        src = (it.get("src") or "").strip()
        if not src or src.startswith("data:"):
            continue
        images.setdefault(norm_link(it["href"]), src)

    print(f"{len(images)} imagenes obtenidas de la home.")
    if not images:
        print("Tarjetas encontradas pero sin URL de imagen utilizable.")
        sys.exit(1)

    old = load_old()
    merged = dict(old)
    merged.update(images)
    merged = dict(list(merged.items())[-MAX_ENTRIES:])

    if merged == old:
        print("Sin cambios.")
        return

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump({"updatedAt": datetime.now().isoformat(), "images": merged}, f, ensure_ascii=False, indent=2)
    print(f"Guardadas {len(merged)} imagenes ({len(images)} de esta ejecucion).")


if __name__ == "__main__":
    main()
