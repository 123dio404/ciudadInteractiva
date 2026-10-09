# Ampliación de una Ciudad Interactiva con OpenGL (11 × 11)

Proyecto práctico de Programación Gráfica (UAGRM) basado en la arquitectura de cuatro clases del Ing. Kenji Kawaida: Java 17, Maven, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core, shaders GLSL, VAO y VBO. Toda la geometría se genera proceduralmente con código (sin texturas ni modelos externos).

## Ejecutar

Abre una terminal en esta carpeta, donde está `pom.xml`:

```sh
# Ejecutar la versión final ampliada con minimapa, entregas y tráfico autónomo:
mvn compile exec:exec

# O ejecutar etapas individuales:
mvn compile exec:exec -DmainClass=com.graphics.clase1
mvn compile exec:exec -DmainClass=com.graphics.clase2
mvn compile exec:exec -DmainClass=com.graphics.clase3
mvn compile exec:exec -DmainClass=com.graphics.clase4
```

*Nota:* Presiona **ESC** para cerrar la ventana. En macOS, el perfil Maven incluye automáticamente `-XstartOnFirstThread`.

## Estructura acumulativa de clases

```text
clase1: ciudad ampliada (11x11, 110x110 u), matriz, renderizador y cámara orbital
  └── clase2 extends clase1: vehículo del jugador, físicas de conducción, colisiones y cámara
        └── clase3 extends clase2: iluminación difusa, día/noche (N), 16 farolas y faros (F)
              └── clase4 extends clase3: decoración urbana, semáforos animados, 4 entregas, minimapa (M) y tráfico autónomo
```

## Características implementadas según el enunciado

1. **Ampliación urbana (11 × 11 celdas):**
   * El mapa se amplió de 7×7 a **11×11 celdas** conservando el tamaño de celda (`CELDA = 10`), abarcando un mundo de **110 × 110 unidades** (`LIMITE = 55`).
   * Distribución: **17 manzanas con edificios** de alturas y colores variados y **8 parques** con césped, árboles y bancos (supera el mínimo exigido de 12 edificios y 4 parques).
   * Red vial continua con calles perimetrales e interiores que conectan todas las manzanas y paradas.
   * Suelo, límites de colisión (`puedeCircular`), cámara orbital y cámara aérea adaptados a la nueva escala.

2. **Iluminación:**
   * Alternancia día y noche con la tecla **N** (luz ambiental azulada y luz direccional atenuada de noche).
   * **16 farolas** distribuidas en los sectores nuevos y periféricos con atenuación cuadrática/lineal y emisión cálida.
   * **Faros delanteros del vehículo** orientados según el giro del auto con cono de luz suave (`smoothstep`) encendibles con **F**.
   * Cálculo de luz difusa mediante normales y producto escalar de Lambert en el fragment shader.

3. **Funciones y decoración urbana:**
   * Parques con árboles (tronco y follaje) y bancos de madera.
   * Edificios con ventanas iluminadas de noche en sus 4 fachadas.
   * Pasos peatonales señalizados junto a las manzanas.
   * Semáforos animados con secuencia automática: Verde (5 s), Amarillo (2 s), Rojo (5 s).
   * **Minimapa simultáneo (tecla M):** Proyección superior ortográfica adaptada a la escala de 110 u (`divisor 58.0`) con encuadre perfecto al redimensionar la ventana, mostrando el auto del jugador (cian/blanco), la baliza dorada de entrega y los autos de tráfico.
   * **Reinicio (tecla R):** Restablece el auto a la esquina suroeste `(-50, 50)`, velocidad, orientación, cronómetro y entregas.

4. **Sistema de entregas:**
   * Recorrido de 4 paradas que atraviesan los cuadrantes de la ciudad ampliada (Noreste `(50, -50)`, Noroeste `(-50, -50)`, Centro comercial `(10, 10)` y retorno Suroeste `(-50, 50)`).
   * Baliza dorada flotante y giratoria en la parada activa; para entregar se debe llegar a menos de 3 unidades y frenar a menos de 1 km/h.

5. **Mejoras opcionales (+10 pts):**
   * **Tráfico autónomo:** 3 vehículos independientes que circulan sin intervención del usuario por las calles (Taxi amarillo en circuito noreste, Patrulla azul en circuito noroeste/central, y Auto verde en circuito sur). Realizan giros en esquinas sin atravesar edificios ni salir del mapa.
   * **Visualización en minimapa:** Los autos de tráfico son visibles en tiempo real en la vista superior con sus colores distintivos.

## Controles

| Tecla | Acción | Etapa |
|---|---|---|
| Flechas Izq / Der | Orbitar la ciudad | Clase 1 |
| W / S o Flechas Arr / Aba | Acelerar / retroceder | Desde Clase 2 |
| A / D o Flechas Izq / Der | Girar al moverse | Desde Clase 2 |
| Espacio | Freno de mano | Desde Clase 2 |
| C | Cámara de seguimiento / aérea general | Desde Clase 2 |
| R | Reiniciar auto, tráfico y entregas | Desde Clase 2 |
| N | Alternar Día / Noche | Desde Clase 3 |
| F | Encender / apagar faros del auto | Desde Clase 3 |
| M | Mostrar / ocultar minimapa | Clase 4 |
| ESC | Salir de la aplicación | Todas |

## Pruebas automatizadas

Para ejecutar las pruebas lógicas unitarias (verificación de calles transitables, colisiones en aceras, límites de 110 u y reinicio):

```sh
mvn test
```

