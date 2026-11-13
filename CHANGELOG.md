# 📋 Changelog - ADRV

Todas las actualizaciones, características, mejoras y correcciones notables de la aplicación **ADRV** quedarán documentadas en este archivo.

---

## [0.2.0] - 2026-11-13

### 🚀 Nuevas Funcionalidades y Mejoras Principales

* **Sistema Inteligente de Novedades y Alertas en Directo:**
  * Implementación de un historial persistente de alertas en la pantalla principal ("Últimas Novedades") que rastrea de forma autónoma cambios de horarios en los próximos partidos y publicaciones de nuevas noticias.
  * Sincronización inteligente de la campana de notificaciones superior con contador dinámico de elementos no leídos y un panel desplegable de avisos.
  * Capacidad de hacer clic en cualquier tarjeta del historial de novedades para abrir directamente el detalle del partido o el visor web de la noticia correspondiente.
* **Integración y Gestión de Múltiples Fuentes de Noticias:**
  * Ampliación del motor de RSS para soportar la web oficial del **Rayo Vallecano**, colectivos de la grada como **Bukaneros** y medios locales independientes.
  * Sistema de filtros por etiquetas/píldoras (Chips) en la pantalla de noticias para ordenar cómodamente la lectura según la fuente preferida.
  * Corrección y optimización en la carga de imágenes (soporte universal de formatos y conversión automática mediante CDN proxy para evitar fallos de renderizado en dispositivos Android antiguos).
* **Calendario, Horarios y Ficha de Partidos Mejorados:**
  * Indicadores visuales específicos en el calendario para distinguir los encuentros con horario oficial confirmado frente a aquellos pendientes de designación.
  * Tarjeta del próximo partido (*NextMatchCard*) optimizada con carga de escudos oficiales en alta resolución, fechas adaptadas al huso horario de Madrid (`Europe/Madrid`), nombre del estadio y competición.
  * Soporte para modales y diálogos flotantes al interactuar con las fechas del calendario.
* **Abono Digital Estilo Wallet & Escaneo Inteligente:**
  * Rediseño estético completo del pase digital para replicar con alta fidelidad la textura y distribución del carnet físico de abonado del Rayo Vallecano.
  * Integración de reconocimiento óptico de caracteres (OCR) y escáner de códigos de barras (Code 128) para la lectura automática de datos a partir de una foto del carnet.
  * Efectos visuales de escaneo en directo y mejoras de rendimiento en la persistencia local de imágenes.
* **Sección "El Equipo" (Clasificación y Plantilla):**
  * Tabla de clasificación de La Liga adaptada en formato compacto con diferenciación por colores de zonas europeas y de descenso.
  * Listado de plantilla completo organizado por demarcaciones (Porteros, Defensas, Centrocampistas y Delanteros) enriquecido con detalles individuales y representación de nacionalidades mediante emojis de banderas.

### 🛠️ Correcciones y Optimización Técnica
* **Arquitectura de Datos Desacoplada (GitHub as a Server):**
  * Automatización mediante **GitHub Actions** y scripts de Python optimizados para consultar periódicamente las APIs de fútbol y feeds RSS, actualizando los archivos JSON estáticos en el repositorio y evitando agotar los límites de cuota gratuitos.
* **Interfaz Moderna con Jetpack Compose y Material 3:**
  * Rediseño de la cabecera institucional (*RayoHeader*) con el escudo oficial centrado, la firma corporativa (`by @Aaronnn17`) y controles de notificaciones limpios.
  * Eliminación de componentes obsoletos de Material 2 y adopción de una navegación fluida, limpia y unificada con la paleta de colores del club (Franja roja y acentos dorados).

---

## [0.1.0] - 2026-11-10

### 🚀 Lanzamiento Inicial
* Estructura base de la aplicación nativa en **Android** desarrollada en **Kotlin** y **Jetpack Compose**.
* Configuración inicial del proyecto, gestión de dependencias y despliegue del repositorio open source.
