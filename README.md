# SurfLog

Aplicación de escritorio en Java/Swing hecha como proyecto final de Programación Orientada a Objetos (universidad). Es un cuaderno de bitácora de spots de surf: das de alta spots con sus características (tipo de fondo, dirección de ola, nivel, tablas recomendadas...), los filtras y consultas en una tabla, y los ves ubicados en un mapa interactivo. Cada spot tiene además un mini-juego de surf jugable, con física propia (aceleración, gravedad, tubos, aéreos) y ambientación visual distinta por spot.

## Qué hace

- **Logbook**: alta/edición/borrado de spots, filtros por país, nivel, tipo de fondo y tabla, tabla con miniatura de foto.
- **Mapa**: los spots posicionados sobre un mapa mundial, con etiquetas.
- **Juego**: por cada spot, un mini-juego de surf con física (aceleración, gravedad, resistencia), tubos, aéreos y puntuación, con paisaje y color de agua propios de cada spot real (Pipeline, Mundaka, Uluwatu, Mawi...).
- **Persistencia**: import/export a JSON, con autoguardado al cerrar.

## Cómo ejecutarlo

Requiere JDK 17+.

```bash
javac -d out -encoding UTF-8 src/*.java
java -cp out Main
```

## Estructura

```
src/
  Main.java                  — punto de entrada, menú, tema visual
  Spot.java                  — modelo de datos de un spot
  PanelPrincipal.java        — tabla, filtros, CRUD
  FormSpot.java               — formulario de alta/edición
  DialogDetalle.java          — ficha de detalle de un spot
  PanelMapa.java               — mapa interactivo
  PersistenciaController.java — import/export JSON (parser propio, sin librerías externas)
  PanelJuego.java              — el mini-juego: física, colisiones, renderizado
  Surfista.java                — el personaje jugable
  ImagenUtil.java               — carga de imágenes compartida
```
