# BiogasGama

App Android (Kotlin + Jetpack Compose) para levantamiento georreferenciado de gases en campo y estimación del potencial de biogás.

- **Metano (BW GasAlertMax XT II):** se ingresa % LEL; la app calcula % vol. = LEL / 20 y ppm = LEL × 500 (100 % LEL = 5,00 % vol. = 50.000 ppm).
- **Otros datos:** H2S y CO (ppm), O2 (% vol.), altitud GNSS (m s. n. m., ajustable), temperatura, viento (m/s o km/h) y dirección.
- **Potencial de biogás SÍ/NO** en tiempo real: CH4 ≥ umbral, O2 < 18 %, H2S detectable y CO bajo. Umbrales configurables en Ajustes.
- **Mapa** (osmdroid, sin clave de API): Google calles, satélite, híbrido y relieve, Esri satélite y OpenStreetMap; posición en vivo y puntos en verde (SÍ) o rojo (NO). Panel de capas: filtro por veredicto, CH4 mínimo y opacidad del mapa base. Las teselas de Google no son un servicio oficial; si dejan de cargar, usar Esri u OSM.
- **GeoPackage:** los puntos se guardan en `biogasgama.gpkg` (capa `puntos_biogas`, Point, EPSG:4326) y se exportan con el selector de archivos o se comparten.

Cada push a `main` compila el APK en GitHub Actions y lo publica en *Releases* (`BiogasGama.apk`). Si la compilación falla, los errores quedan en la rama `registro`.
