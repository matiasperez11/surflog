# SurfLog — Proyecto Final POO

## Descripción del proyecto

**SurfLog** es una aplicación de escritorio en Java/Swing para gestionar spots de surf como un cuaderno de bitácora personal. El usuario puede registrar spots, valorarlos, filtrarlos y llevar un historial de visitas.

**Fecha de entrega:** 24 de mayo de 2026 (último examen).

---

## Objeto principal: `Spot`

Atributos del modelo:

| Campo | Tipo |
|---|---|
| nombre | String |
| pais | String |
| region | String |
| tipoFondo | TipoFondo (enum) |
| direccionOla | DireccionOla (enum) |
| nivelDificultad | NivelDificultad (enum) |
| tablaRecomendada | TablaRecomendada (enum) |
| mejorEpoca | String |
| rutaFoto | String |
| comentario | String |
| valoracion | int (1–5) |
| visitado | boolean |
| fechaVisita | LocalDate |

### Enums

```
TipoFondo:       BEACH_BREAK, REEF_BREAK, POINT_BREAK, RIVER_MOUTH
DireccionOla:    IZQUIERDA, DERECHA, AMBAS, SHORE_BREAK, CLOSEOUT
NivelDificultad: PRINCIPIANTE, INTERMEDIO, AVANZADO, EXPERTO
TablaRecomendada: SHORTBOARD, FISH, FUNBOARD, MIDLENGTH, LONGBOARD, GUN
```

---

## Estructura de paquetes

```
src/
├── model/
│   ├── Spot.java
│   ├── TipoFondo.java
│   ├── DireccionOla.java
│   ├── NivelDificultad.java
│   └── TablaRecomendada.java
├── controller/
│   ├── SpotController.java      ← CRUD + filtros
│   └── PersistenciaController.java  ← JSON import/export
└── view/
    ├── MainFrame.java           ← JFrame principal con JTabbedPane
    ├── TablaSpots.java          ← JTable con todos los spots
    ├── FormSpot.java            ← Formulario alta/edición
    ├── PanelFiltros.java        ← JComboBox para filtros
    ├── PanelDetalle.java        ← Foto + comentarios del spot
    └── PanelRecomendados.java   ← Filtrar por tabla o nivel
```

---

## Requisitos funcionales (enunciado)

- **CRUD completo**: alta, baja, modificación de spots
- **Filtros** (≥2 criterios): por país, nivel de dificultad, tipo de fondo, tabla recomendada
- **Persistencia en disco**: guardar/cargar automáticamente en JSON
- **Importar/Exportar JSON**: opción explícita en menú o botón
- **Threading**: carga inicial de datos en hilo separado (no bloquear la UI)
- **Componentes nuevos obligatorios**:
  - `JTable` (puntuación alta: +1.5 pts)
  - `JTree` árbol de spots por país → región (puntuación alta: +1.5 pts)
  - `JTabbedPane` para organizar vistas
  - `JComboBox` para filtros de enums

---

## Evaluación (base 9.0, máx 10.5)

```
funcionalidadCompleta == NO  →  nota = 0
necesidadReal == NO          →  nota -= 3
componentesNuevos == NO      →  nota = 0
  JTable / JTree (alto)      →  nota += 1.5 c/u
errores de ejecución         →  nota -= 1.5
inicio no sencillo           →  nota -= 1
usabilidad mala              →  nota -= 2
diseño malo                  →  nota -= 1.5
diseño regular               →  nota -= 0.75
```

**Entregables (ZIP):**
- `Proyecto.pdf` — una página con pantallazos
- `Sources.zip` — .java en estructura de paquetes
- `Bin.zip` — JAR ejecutable (`java -jar SurfLog.jar`)
- `README.txt` — usuarios/contraseñas si los hay

---

## Compilación y estructura del proyecto

**Sin herramientas de build** (sin Maven, sin Gradle, sin pom.xml). Igual que los proyectos de referencia en `../practiasm` y `../javaCourseExamples2026`: compilación directa con `javac`, sin dependencias externas, solo JDK estándar.

**Sin librerías externas**: no GSON, no org.json, nada. El JSON se serializa/deserializa a mano en `PersistenciaController`.

### Compilar y ejecutar (desde `game/`)

```bash
# Compilar todo
javac -d out src/model/*.java src/controller/*.java src/view/*.java src/Main.java

# Ejecutar
java -cp out Main
```

### Empaquetar JAR para entrega

```bash
# Crear el jar ejecutable
jar cfm dist/SurfLog.jar manifest.txt -C out .

# manifest.txt contiene:
# Main-Class: Main
```

El fichero `spots.json` se crea en el mismo directorio desde donde se lanza el JAR.

---

## Estilo de código — seguir siempre

Basado en el código de referencia en `../practiasm` (JPelota, JPong, agario, juego_1, juego_2, examen_inter).

### Nombres

- **Clases**: PascalCase — `SpotController`, `MainFrame`, `TablaSpots`
- **Variables y métodos**: camelCase — `nivelDificultad`, `cargarDatos()`, `aplicarFiltros()`
- **Constantes**: UPPER_SNAKE_CASE con `public static final` — `MAX_VALORACION`, `FICHERO_DATOS`
- **Mezcla español/inglés** es aceptable igual que en los juegos de referencia; preferir español para el dominio (`valoracion`, `visitado`, `tipoFondo`) e inglés para infraestructura técnica
- Algunos métodos pueden usar snake_case si es coherente con el estilo heredado (`check_filtros`, `load_data`)

### Estructura de clases

Orden dentro de cada clase:

1. Constantes `static final`
2. Campos de instancia (preferir **public** salvo que la herencia exija `protected`)
3. Constructor(es)
4. Métodos principales de negocio
5. Métodos auxiliares / utilidades
6. `toString()` y overrides

### Visibilidad

- **Campos public** por defecto (igual que en los juegos: `public int x`, `public Color color`)
- `protected` para herencia
- `private` sólo para estado verdaderamente interno (e.g., `private static Random rand`)
- No abusar de getters/setters si el acceso directo es suficiente

### Comentarios

- **Sin Javadoc** salvo los 10 elementos obligatorios del enunciado
- Comentarios inline mínimos, sólo cuando el WHY no es obvio
- En español cuando se escriban: `// cargamos en hilo para no bloquear la UI`
- No comentar código evidente; no bloques de comentarios multilinea

### Swing y listeners

- **Clases anónimas** para listeners simples (KeyAdapter, MouseAdapter, ActionListener):
  ```java
  boton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          controller.guardar();
      }
  });
  ```
- **Lambdas** para hilos y streams:
  ```java
  hilo = new Thread(() -> {
      controller.cargarDatos();
      SwingUtilities.invokeLater(() -> tabla.refresh());
  });
  hilo.start();
  ```
- No crear clases listener separadas salvo que sean muy complejas

### Threading

- Crear `Thread` con lambda para carga inicial, no bloquear el EDT
- Usar `SwingUtilities.invokeLater()` para actualizar la UI desde hilos secundarios
- Try-catch con `e.printStackTrace()` en el `Thread.sleep()`:
  ```java
  try {
      Thread.sleep(200);
  } catch (InterruptedException e) {
      e.printStackTrace();
  }
  ```

### Colecciones

- `ArrayList<Spot>` como estructura principal
- `HashMap` o `TreeMap` para agrupar por país/región en el JTree
- Streams con lambdas para filtrar: `lista.stream().filter(s -> s.pais.equals(pais)).collect(...)`
- `removeIf` para bajas: `spots.removeIf(s -> s.nombre.equals(nombre))`

### Imports

- Wildcards para paquetes usados extensamente: `import java.awt.*; import javax.swing.*; import java.util.*;`
- Imports explícitos para clases concretas poco usadas

### Formato

- Llaves de apertura en la misma línea: `public void metodo() {`
- Indentación: 4 espacios
- Espacios alrededor de operadores: `valoracion += 1`, `if (x > 0)`
- Una variable por línea en declaraciones de campos

### Patrones de diseño

El enunciado exige MVC/DAO — aplicar así:

- **Model**: `Spot` y enums son datos puros, sin lógica de UI
- **Controller**: `SpotController` gestiona la lista en memoria + filtros; `PersistenciaController` maneja JSON
- **View**: clases `view/` no acceden directamente al modelo, usan el controller
- Seguir **KISS**: no sobre-ingenierizar; si un método de 10 líneas resuelve el problema, no crear una jerarquía de clases
- Seguir **DRY**: reutilizar métodos de filtrado, no duplicar lógica de búsqueda

---

## Notas de implementación

- Persistencia en `spots.json` en el directorio de trabajo del JAR
- El JTree agrupa: raíz → país → región → nombre del spot
- El JTable muestra: nombre, país, nivel, tipo de fondo, valoración (★), visitado
- `PanelDetalle` carga la foto con `ImageIcon` escalada; si la ruta no existe, muestra placeholder
- Los JComboBox de filtros incluyen siempre la opción `"Todos"` como primer ítem
- Valoración se muestra como estrellas (★☆) en la tabla para usabilidad visual
