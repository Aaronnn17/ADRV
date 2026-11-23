import json
import os
import time
import unicodedata
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
import requests

MADRID_TZ = ZoneInfo("Europe/Madrid")

# Coordenadas exactas de estadios (LaLiga y posibles rivales)
STADIUM_COORDS = {
    "rayo": {"lat": 40.3919, "lon": -3.6589, "ciudad": "Madrid (Vallecas)", "estadio": "Estadio de Vallecas"},
    "real madrid": {"lat": 40.4530, "lon": -3.6883, "ciudad": "Madrid", "estadio": "Santiago Bernabéu"},
    "atletico": {"lat": 40.4362, "lon": -3.5995, "ciudad": "Madrid", "estadio": "Riyadh Air Metropolitano"},
    "getafe": {"lat": 40.3256, "lon": -3.7149, "ciudad": "Getafe", "estadio": "Coliseum"},
    "leganes": {"lat": 40.3404, "lon": -3.7607, "ciudad": "Leganés", "estadio": "Butarque"},
    "barcelona": {"lat": 41.3809, "lon": 2.1228, "ciudad": "Barcelona", "estadio": "Spotify Camp Nou"},
    "espanyol": {"lat": 41.3479, "lon": 2.0757, "ciudad": "Cornellà de Llobregat", "estadio": "RCDE Stadium"},
    "girona": {"lat": 41.9614, "lon": 2.8286, "ciudad": "Girona", "estadio": "Montilivi"},
    "valencia": {"lat": 39.4747, "lon": -0.3583, "ciudad": "Valencia", "estadio": "Mestalla"},
    "levante": {"lat": 39.4947, "lon": -0.3642, "ciudad": "Valencia", "estadio": "Ciutat de València"},
    "villarreal": {"lat": 39.9442, "lon": -0.1035, "ciudad": "Vila-real", "estadio": "Estadio de la Cerámica"},
    "elche": {"lat": 38.2670, "lon": -0.6633, "ciudad": "Elche", "estadio": "Martínez Valero"},
    "sevilla": {"lat": 37.3841, "lon": -5.9707, "ciudad": "Sevilla", "estadio": "Ramón Sánchez-Pizjuán"},
    "betis": {"lat": 37.3565, "lon": -5.9818, "ciudad": "Sevilla", "estadio": "Benito Villamarín"},
    "malaga": {"lat": 36.7336, "lon": -4.4264, "ciudad": "Málaga", "estadio": "La Rosaleda"},
    "granada": {"lat": 37.1529, "lon": -3.5957, "ciudad": "Granada", "estadio": "Nuevo Los Cármenes"},
    "cadiz": {"lat": 36.5027, "lon": -6.2729, "ciudad": "Cádiz", "estadio": "Nuevo Mirandilla"},
    "almeria": {"lat": 36.8400, "lon": -2.4353, "ciudad": "Almería", "estadio": "UD Almería Stadium"},
    "recreativo": {"lat": 37.2464, "lon": -6.9547, "ciudad": "Huelva", "estadio": "Nuevo Colombino"},
    "athletic": {"lat": 43.2641, "lon": -2.9494, "ciudad": "Bilbao", "estadio": "San Mamés"},
    "sociedad": {"lat": 43.3014, "lon": -1.9736, "ciudad": "San Sebastián", "estadio": "Reale Arena"},
    "alaves": {"lat": 42.8371, "lon": -2.6880, "ciudad": "Vitoria-Gasteiz", "estadio": "Mendizorrotza"},
    "osasuna": {"lat": 42.7967, "lon": -1.6370, "ciudad": "Pamplona", "estadio": "El Sadar"},
    "celta": {"lat": 42.2118, "lon": -8.7397, "ciudad": "Vigo", "estadio": "Abanca Balaídos"},
    "deportivo": {"lat": 43.3687, "lon": -8.4175, "ciudad": "A Coruña", "estadio": "Abanca-Riazor"},
    "mallorca": {"lat": 39.5900, "lon": 2.6300, "ciudad": "Palma", "estadio": "Estadi Mallorca Son Moix"},
    "las palmas": {"lat": 28.1004, "lon": -15.4567, "ciudad": "Las Palmas", "estadio": "Estadio de Gran Canaria"},
    "valladolid": {"lat": 41.6444, "lon": -4.7612, "ciudad": "Valladolid", "estadio": "José Zorrilla"},
    "racing": {"lat": 43.4764, "lon": -3.7933, "ciudad": "Santander", "estadio": "El Sardinero"},
    "oviedo": {"lat": 43.3608, "lon": -5.8702, "ciudad": "Oviedo", "estadio": "Carlos Tartiere"},
    "sporting": {"lat": 43.5361, "lon": -5.6373, "ciudad": "Gijón", "estadio": "El Molinón"},
    "zaragoza": {"lat": 41.6366, "lon": -0.9018, "ciudad": "Zaragoza", "estadio": "La Romareda"},
}

WMO_CODES = {
    0: ("Despejado", "clear"),
    1: ("Mayormente despejado", "mainly_clear"),
    2: ("Parcialmente nublado", "partly_cloudy"),
    3: ("Nublado", "cloudy"),
    45: ("Niebla", "fog"),
    48: ("Niebla con escarcha", "fog"),
    51: ("Llovizna ligera", "drizzle"),
    53: ("Llovizna moderada", "drizzle"),
    55: ("Llovizna intensa", "drizzle"),
    56: ("Llovizna helada ligera", "drizzle"),
    57: ("Llovizna helada intensa", "drizzle"),
    61: ("Lluvia débil", "rain"),
    63: ("Lluvia moderada", "rain"),
    65: ("Lluvia intensa", "heavy_rain"),
    66: ("Lluvia helada débil", "rain"),
    67: ("Lluvia helada fuerte", "heavy_rain"),
    71: ("Nieve ligera", "snow"),
    73: ("Nieve moderada", "snow"),
    75: ("Nieve intensa", "snow"),
    77: ("Granizo fino", "snow"),
    80: ("Chubascos débiles", "rain"),
    81: ("Chubascos moderados", "rain"),
    82: ("Chubascos fuertes", "heavy_rain"),
    85: ("Chubascos de nieve", "snow"),
    86: ("Chubascos fuertes de nieve", "snow"),
    95: ("Tormenta", "storm"),
    96: ("Tormenta con granizo", "storm"),
    99: ("Tormenta fuerte con granizo", "storm"),
}


def normalize_text(text: str) -> str:
    if not text:
        return ""
    nfkd = unicodedata.normalize("NFKD", text.lower().strip())
    return "".join(c for c in nfkd if not unicodedata.combining(c))


def degrees_to_cardinal(deg):
    if deg is None:
        return ""
    dirs = ["N", "NE", "E", "SE", "S", "SO", "O", "NO"]
    idx = round(deg / 45) % 8
    return dirs[idx]


def decode_wmo(code):
    return WMO_CODES.get(code, ("Variable", "partly_cloudy"))


def resolve_location(home_team: str, venue: str):
    home_norm = normalize_text(home_team)
    venue_norm = normalize_text(venue or "")

    for key, info in STADIUM_COORDS.items():
        if key in home_norm or key in venue_norm:
            result = dict(info)
            if venue and venue != "Estadio por confirmar" and venue != "Estadio Visitante":
                result["estadio"] = venue
            return result

    # Fallback: buscar coordenadas por nombre de equipo en la API de geocoding de Open-Meteo
    try:
        query_name = home_team.replace("CF", "").replace("CD", "").replace("UD", "").replace("RC", "").strip()
        geo_res = requests.get(
            "https://geocoding-api.open-meteo.com/v1/search",
            params={"name": query_name, "count": 1, "language": "es", "format": "json"},
            timeout=10,
        )
        if geo_res.status_code == 200:
            results = geo_res.json().get("results", [])
            if results:
                first = results[0]
                return {
                    "lat": first["latitude"],
                    "lon": first["longitude"],
                    "ciudad": first.get("name", query_name),
                    "estadio": venue or f"Estadio de {home_team}",
                }
    except Exception as e:
        print(f"Aviso en geocoding: {e}")

    # Último recurso: Estadio de Vallecas
    return dict(STADIUM_COORDS["rayo"])


def find_next_match():
    if not os.path.exists("matches.json"):
        print("No se encontró matches.json")
        return None

    with open("matches.json", "r", encoding="utf-8") as f:
        data = json.load(f)

    partidos = data.get("partidos", [])
    now_utc = datetime.now(timezone.utc)
    upcoming = []

    for m in partidos:
        if m.get("status") == "FINISHED":
            continue
        date_str = m.get("dateText", "")
        try:
            dt_utc = datetime.fromisoformat(date_str.replace("Z", "+00:00"))
            # Incluimos partidos futuros o que hayan empezado hace menos de 3 horas
            if (dt_utc - now_utc).total_seconds() > -10800:
                upcoming.append((dt_utc, m))
        except Exception:
            continue

    if not upcoming:
        return None

    upcoming.sort(key=lambda x: x[0])
    return upcoming[0]


def build_stadium_advice(temp, feels_like, rain_prob, rain_mm, wind_kmh, uv, is_day):
    if rain_prob is not None and (rain_prob >= 60 or (rain_mm or 0) >= 1.0):
        return "Alta probabilidad de lluvia: lleva chubasquero o paraguas plegable."
    if rain_prob is not None and rain_prob >= 30:
        return "Posibles chubascos: conviene llevar prenda impermeable ligera."
    if feels_like is not None and feels_like <= 8:
        return "Frío notable en la grada: abrigo grueso, bufanda y guantes."
    if feels_like is not None and feels_like <= 14:
        return "Ambiente fresco: lleva chaqueta o sudadera, refrescará en la 2ª parte."
    if wind_kmh is not None and wind_kmh >= 30:
        return "Rachas fuertes de viento: lleva cortavientos."
    if is_day == 1 and uv is not None and uv >= 6:
        return "Sol intenso en el arranque: gorra, gafas de sol y protección."
    if temp is not None and temp >= 28:
        return "Calor en el estadio: ropa ligera e hidratación."
    return "Condiciones ideales para disfrutar del partido en la grada."


def main():
    next_info = find_next_match()
    now_madrid = datetime.now(MADRID_TZ).isoformat()

    if not next_info:
        output = {
            "actualizado_en": now_madrid,
            "disponible": False,
            "motivo": "No hay próximos partidos programados.",
        }
        with open("weather.json", "w", encoding="utf-8") as f:
            json.dump(output, f, indent=4, ensure_ascii=False)
        print("weather.json guardado (sin próximo partido).")
        return

    match_dt_utc, match = next_info
    match_dt_madrid = match_dt_utc.astimezone(MADRID_TZ)
    match_date_str = match_dt_madrid.strftime("%Y-%m-%d")
    match_hour_str = match_dt_madrid.strftime("%Y-%m-%dT%H:00")

    days_ahead = (match_dt_madrid.date() - datetime.now(MADRID_TZ).date()).days
    location = resolve_location(match.get("homeTeam", ""), match.get("venue", ""))

    base_match_info = {
        "id": match.get("id", ""),
        "homeTeam": match.get("homeTeam", ""),
        "awayTeam": match.get("awayTeam", ""),
        "dateText": match.get("dateText", ""),
        "fecha_local": match_date_str,
        "hora_local": match_dt_madrid.strftime("%H:%M"),
        "estadio": location["estadio"],
        "ciudad": location["ciudad"],
        "lat": location["lat"],
        "lon": location["lon"],
    }

    if days_ahead > 15:
        output = {
            "actualizado_en": now_madrid,
            "disponible": False,
            "motivo": f"Pronóstico disponible a partir de 15 días antes del partido (faltan {days_ahead} días).",
            "partido": base_match_info,
        }
        with open("weather.json", "w", encoding="utf-8") as f:
            json.dump(output, f, indent=4, ensure_ascii=False)
        print(f"Partido a {days_ahead} días: fuera de ventana de 16 días.")
        return

    # Pedimos solo el día del partido y el siguiente (mucho más rápido que 16 días)
    next_day_str = (match_dt_madrid.date() + timedelta(days=1)).strftime("%Y-%m-%d")

    params = {
        "latitude": location["lat"],
        "longitude": location["lon"],
        "timezone": "Europe/Madrid",
        "start_date": match_date_str,
        "end_date": next_day_str,
        "hourly": ",".join([
            "temperature_2m",
            "apparent_temperature",
            "relative_humidity_2m",
            "precipitation_probability",
            "precipitation",
            "rain",
            "showers",
            "weather_code",
            "cloud_cover",
            "wind_speed_10m",
            "wind_gusts_10m",
            "wind_direction_10m",
            "uv_index",
            "visibility",
            "is_day",
        ]),
        "daily": ",".join([
            "weather_code",
            "temperature_2m_max",
            "temperature_2m_min",
            "apparent_temperature_max",
            "apparent_temperature_min",
            "sunrise",
            "sunset",
            "uv_index_max",
            "precipitation_sum",
            "precipitation_probability_max",
            "wind_speed_10m_max",
            "wind_gusts_10m_max",
        ]),
    }

    headers = {"User-Agent": "ADRV-App-Weather/1.0"}
    wdata = None

    for attempt in range(1, 4):
        try:
            res = requests.get(
                "https://api.open-meteo.com/v1/forecast",
                params=params,
                headers=headers,
                timeout=30,
            )
            res.raise_for_status()
            wdata = res.json()
            break
        except Exception as e:
            print(f"Intento {attempt}/3 fallido consultando Open-Meteo: {e}")
            if attempt < 3:
                time.sleep(3 * attempt)

    if not wdata:
        print("No se pudo actualizar el tiempo en esta ejecución; se mantiene el estado anterior.")
        if not os.path.exists("weather.json"):
            fallback = {
                "actualizado_en": now_madrid,
                "disponible": False,
                "motivo": "Servicio meteorológico temporalmente no disponible.",
                "partido": base_match_info,
            }
            with open("weather.json", "w", encoding="utf-8") as f:
                json.dump(fallback, f, indent=4, ensure_ascii=False)
        return
    hourly = wdata.get("hourly", {})
    daily = wdata.get("daily", {})

    times = hourly.get("time", [])
    daily_times = daily.get("time", [])

    if match_hour_str not in times:
        output = {
            "actualizado_en": now_madrid,
            "disponible": False,
            "motivo": "Hora del partido no disponible todavía en la previsión.",
            "partido": base_match_info,
        }
        with open("weather.json", "w", encoding="utf-8") as f:
            json.dump(output, f, indent=4, ensure_ascii=False)
        print("Hora no encontrada en Open-Meteo.")
        return
        

    idx = times.index(match_hour_str)

    def build_hour_entry(i: int):
        wcode = hourly["weather_code"][i]
        desc, icon_key = decode_wmo(wcode)
        vis_m = hourly["visibility"][i]
        wind_deg = hourly["wind_direction_10m"][i]
        return {
            "hora": times[i].split("T")[1],
            "temperatura": round(hourly["temperature_2m"][i], 1) if hourly["temperature_2m"][i] is not None else None,
            "sensacion_termica": round(hourly["apparent_temperature"][i], 1) if hourly["apparent_temperature"][i] is not None else None,
            "humedad": hourly["relative_humidity_2m"][i],
            "probabilidad_lluvia": hourly["precipitation_probability"][i],
            "precipitacion_mm": round(hourly["precipitation"][i], 2) if hourly["precipitation"][i] is not None else 0.0,
            "lluvia_mm": round(hourly["rain"][i], 2) if hourly["rain"][i] is not None else 0.0,
            "chubascos_mm": round(hourly["showers"][i], 2) if hourly["showers"][i] is not None else 0.0,
            "nubosidad": hourly["cloud_cover"][i],
            "viento_kmh": round(hourly["wind_speed_10m"][i], 1) if hourly["wind_speed_10m"][i] is not None else None,
            "rachas_viento_kmh": round(hourly["wind_gusts_10m"][i], 1) if hourly["wind_gusts_10m"][i] is not None else None,
            "direccion_viento_grados": wind_deg,
            "direccion_viento": degrees_to_cardinal(wind_deg),
            "indice_uv": round(hourly["uv_index"][i], 1) if hourly["uv_index"][i] is not None else None,
            "visibilidad_km": round(vis_m / 1000.0, 1) if vis_m is not None else None,
            "es_dia": hourly["is_day"][i] == 1,
            "codigo_wmo": wcode,
            "descripcion": desc,
            "icono": icon_key,
        }

    # Datos exactos a la hora del saque inicial
    hora_partido = build_hour_entry(idx)

    # Franja completa: desde 2 horas antes (previa) hasta 3 horas después (salida del estadio)
    start_idx = max(0, idx - 2)
    end_idx = min(len(times) - 1, idx + 3)
    evolucion_horaria = []
    for i in range(start_idx, end_idx + 1):
        entry = build_hour_entry(i)
        diff = i - idx
        if diff == -2:
            etiqueta = "Previa (-2h)"
        elif diff == -1:
            etiqueta = "Previa (-1h)"
        elif diff == 0:
            etiqueta = "Inicio"
        elif diff == 1:
            etiqueta = "2ª Parte"
        elif diff == 2:
            etiqueta = "Final"
        else:
            etiqueta = "Salida"
        entry["etiqueta"] = etiqueta
        evolucion_horaria.append(entry)

    # Resumen diario del día de partido
    resumen_dia = {}
    if match_date_str in daily_times:
        d_idx = daily_times.index(match_date_str)
        d_code = daily["weather_code"][d_idx]
        d_desc, d_icon = decode_wmo(d_code)
        sunrise_raw = daily["sunrise"][d_idx]
        sunset_raw = daily["sunset"][d_idx]
        resumen_dia = {
            "temp_max": round(daily["temperature_2m_max"][d_idx], 1) if daily["temperature_2m_max"][d_idx] is not None else None,
            "temp_min": round(daily["temperature_2m_min"][d_idx], 1) if daily["temperature_2m_min"][d_idx] is not None else None,
            "sensacion_max": round(daily["apparent_temperature_max"][d_idx], 1) if daily["apparent_temperature_max"][d_idx] is not None else None,
            "sensacion_min": round(daily["apparent_temperature_min"][d_idx], 1) if daily["apparent_temperature_min"][d_idx] is not None else None,
            "prob_lluvia_max": daily["precipitation_probability_max"][d_idx],
            "precipitacion_total_mm": round(daily["precipitation_sum"][d_idx], 2) if daily["precipitation_sum"][d_idx] is not None else 0.0,
            "viento_max_kmh": round(daily["wind_speed_10m_max"][d_idx], 1) if daily["wind_speed_10m_max"][d_idx] is not None else None,
            "rachas_max_kmh": round(daily["wind_gusts_10m_max"][d_idx], 1) if daily["wind_gusts_10m_max"][d_idx] is not None else None,
            "uv_max": round(daily["uv_index_max"][d_idx], 1) if daily["uv_index_max"][d_idx] is not None else None,
            "amanecer": sunrise_raw.split("T")[1] if sunrise_raw and "T" in sunrise_raw else "",
            "atardecer": sunset_raw.split("T")[1] if sunset_raw and "T" in sunset_raw else "",
            "codigo_wmo": d_code,
            "descripcion": d_desc,
            "icono": d_icon,
        }

    consejo = build_stadium_advice(
        temp=hora_partido["temperatura"],
        feels_like=hora_partido["sensacion_termica"],
        rain_prob=hora_partido["probabilidad_lluvia"],
        rain_mm=hora_partido["precipitacion_mm"],
        wind_kmh=hora_partido["viento_kmh"],
        uv=hora_partido["indice_uv"],
        is_day=1 if hora_partido["es_dia"] else 0,
    )

    output = {
        "actualizado_en": now_madrid,
        "disponible": True,
        "partido": base_match_info,
        "hora_partido": hora_partido,
        "resumen_dia": resumen_dia,
        "evolucion_horaria": evolucion_horaria,
        "consejo_grada": consejo,
    }

    with open("weather.json", "w", encoding="utf-8") as f:
        json.dump(output, f, indent=4, ensure_ascii=False)

    print(
        f"¡weather.json actualizado! {base_match_info['homeTeam']} vs {base_match_info['awayTeam']} "
        f"({base_match_info['ciudad']}) -> {hora_partido['temperatura']}°C, "
        f"{hora_partido['descripcion']}, lluvia {hora_partido['probabilidad_lluvia']}%"
    )


if __name__ == "__main__":
    main()
