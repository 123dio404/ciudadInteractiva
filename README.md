# Ampliación de una Ciudad Interactiva con OpenGL (11 × 11)

**Actividad práctica de Programación Gráfica — UAGRM**  
Documento descriptivo y técnico del proyecto según los requisitos de entrega del enunciado.

---

## 1. Instrucciones de ejecución

### Requisitos previos
* **Java Development Kit (JDK):** Versión 17 o superior.
* **Apache Maven:** Versión 3.8 o superior.
* Tarjeta gráfica compatible con **OpenGL 3.3 Core Profile**.

### Comando exacto para ejecutar la versión final
Desde la terminal, situarse en el directorio raíz del proyecto (donde se encuentra `pom.xml`) y ejecutar:

```bash
mvn compile exec:exec
```

*(El archivo `pom.xml` tiene configurada por defecto la clase principal final `com.graphics.clase4`).*

### Ejecución de etapas individuales (desarrollo progresivo)
Si se desea verificar cada etapa de la arquitectura acumulativa:

```bash
# Etapa 1: Ciudad ampliada (11x11), matriz urbana y cámara orbital
mvn compile exec:exec -DmainClass=com.graphics.clase1

# Etapa 2: Físicas de conducción, colisiones y cámara de seguimiento
mvn compile exec:exec -DmainClass=com.graphics.clase2

# Etapa 3: Iluminación diurna/nocturna, farolas del entorno y faros del vehículo
mvn compile exec:exec -DmainClass=com.graphics.clase3

# Etapa 4: Versión final con decoración, semáforos, minimapa, entregas y tráfico
mvn compile exec:exec -DmainClass=com.graphics.clase4
```

### Pruebas unitarias automatizadas
Para comprobar la consistencia lógica de las calles transitables, límites de 110 unidades y detección de colisiones:

```bash
mvn test
```

### Limpieza de archivos temporales previa a la entrega
Para cumplir con la exclusión obligatoria de dependencias descargadas y temporales antes del empaquetado/entrega:

```bash
mvn clean
```

---

## 2. Descripción del diseño de la ciudad

* **Dimensiones y escala:**
  * El mapa fue ampliado de la cuadrícula original de 7 × 7 a **11 × 11 celdas**, conservando el tamaño de cada celda (`CELDA = 10` unidades).
  * Abarca un espacio tridimensional total de **110 × 110 unidades** en el plano XZ, con límites de circulación entre `[-55, 55]`.
* **Distribución urbana:**
  * **17 manzanas con edificios:** Variedad de alturas (desde edificios bajos de 6 unidades hasta torres de 24 unidades) y paletas cromáticas diferenciadas (tonos corporativos, residenciales y comerciales).
  * **8 parques urbanos:** Distribuidos equilibradamente en cuadrantes periféricos e interiores, con áreas de césped verde, árboles modelados proceduralmente (tronco y copa) y bancos de descanso de madera.
  * Supera ampliamente el mínimo exigido por el enunciado (12 manzanas de edificios y 4 parques).
* **Red vial y conectividad:**
  * Cuadrícula de calles vehiculares continuas (`CALLE = 0`), con vías perimetrales e interiores interconectadas.
  * Garantiza el libre acceso vehicular a todos los cuadrantes, manzanas y puntos de entrega sin callejones sin salida.
* **Coherencia del escenario:**
  * El plano del suelo de asfalto (`y = 0`), las aceras elevadas (`y = 0.3`), los límites del mundo, el algoritmo de colisión AABB (`puedeCircular`), las cámaras de proyección y el visor del minimapa fueron completamente adaptados a la nueva escala de 110 unidades.

---

## 3. Controles

| Tecla / Control | Función | Etapa disponible |
|---|---|---|
| **W** / **Flecha Arriba** | Acelerar el vehículo hacia adelante | Desde Clase 2 |
| **S** / **Flecha Abajo** | Frenar y retroceder | Desde Clase 2 |
| **A** / **Flecha Izquierda** | Girar dirección a la izquierda (con movimiento) | Desde Clase 2 |
| **D** / **Flecha Derecha** | Girar dirección a la derecha (con movimiento) | Desde Clase 2 |
| **Espacio** | Freno de mano instantáneo | Desde Clase 2 |
| **C** | Alternar vista de cámara (Seguimiento en 3ª persona / Aérea cenital) | Desde Clase 2 |
| **Flechas Izq / Der** *(en Clase 1)* | Orbitar la cámara alrededor de la ciudad | Clase 1 |
| **N** | Alternar modo ambiental: **Día** / **Noche** | Desde Clase 3 |
| **F** | Encender / apagar faros delanteros del automóvil | Desde Clase 3 |
| **M** | Mostrar / ocultar minimapa simultáneo superior | Clase 4 |
| **P** | Pausar / reanudar la simulación | Clase 4 |
| **R** | **Reiniciar**: restablece posición inicial, orientación, velocidad, entregas y tráfico | Desde Clase 2 |
| **ESC** | Cerrar la aplicación de forma segura | Todas |

---

## 4. Funciones implementadas

### Arquitectura acumulativa de clases
1. **`clase1` (Escenario base y renderizado):** Creación del buffer de vértices (VAO/VBO), matriz urbana 11×11, renderizado de edificios, suelo, aceras y cámara orbital.
2. **`clase2` (Conducción y cámara):** Físicas de aceleración, rozamiento, marcha atrás, cálculo de colisiones contra aceras/edificios (`puedeCircular`) y cámara de seguimiento en 3ra persona.
3. **`clase3` (Iluminación avanzada):**
   * **Modo Día/Noche (tecla N):** Luz direccional solar con tono cálido de día y luz ambiental azulada nocturna que preserva visibilidad para conducir.
   * **16 Farolas urbanas:** Distribuidas estratégicamente en calles y avenidas. Cuentan con atenuación radial cuadrática que ilumina tanto el pavimento como las fachadas de los edificios colindantes.
   * **Faros del vehículo (tecla F):** Dos focos frontales fijados a la posición y ángulo del auto, con proyección cónica suave (`smoothstep`) y alcance limitado.
   * **Cálculo difuso:** Sombreado de Lambert basado en normales de superficie calculado en el *fragment shader*.
4. **`clase4` (Versión final completa):**
   * **Decoración urbana:** Árboles y bancos en los parques, pasos peatonales en esquinas y ventanas en las fachadas de los edificios que simulan iluminación interna durante la noche.
   * **Semáforos animados:** Temporizador cíclico autónomo con fases de Verde (5 s), Amarillo (2 s) y Rojo (5 s).
   * **Minimapa simultáneo (tecla M):** Proyección ortográfica superior en la esquina superior derecha, orientada con el norte hacia arriba, encuadre constante independiente del redimensionamiento de ventana y representación en tiempo real del jugador, baliza y tráfico.
   * **Sistema de entregas:** 4 paradas consecutivas que recorren los cuadrantes de la ciudad ampliada (Noreste, Noroeste, Centro comercial y retorno Suroeste) indicadas con una baliza dorada flotante.
   * **Reinicio completo (tecla R):** Restablece el auto a la posición inicial `(-50, 50)`, velocidad a cero y reinicia el circuito de entregas.
   * **Mejora opcional — Tráfico autónomo:** 3 vehículos controlados por computadora (un taxi amarillo, una patrulla azul y un sedán verde) que recorren rutas prefijadas por las calles realizando giros en intersecciones sin atravesar edificios ni salir del mapa.

---

## 5. Limitaciones

* **Geometría puramente procedural (sin modelos externos ni mallas complejas):** Toda la geometría (vehículos, árboles, farolas, edificios) está construida mediante primitivas cúbicas y prismáticas directamente en memoria de vértices; no se utilizan archivos externos OBJ, FBX ni texturas de imagen rasterizadas (PNG/JPG).
* **Física y colisiones simplificadas:** La detección de colisiones opera en el plano XZ usando comprobaciones AABB basadas en celdas de cuadrícula. No implementa físicas de cuerpo rígido con masa o momento de inercia angular ni deformación de malla.
* **Iluminación sin sombreado proyectado (Shadow Mapping):** El cálculo de iluminación calcula aporte difuso y atenuación radial/cónica, pero no proyecta sombras dinámicas entre objetos (evitando el sobrecosto de pases de profundidad adicionales).
* **Semáforos de carácter visual:** Los semáforos cambian de estado de forma realista mediante temporizadores, pero no detienen mecánicamente a los vehículos de tráfico ni al jugador (cumpliendo con lo indicado en el enunciado: *"El alcance obligatorio es visual: no se exige que detengan al automóvil"*).

---

## 6. Recursos y fragmentos externos utilizados

* **Código de referencia y arquitectura:**
  * Estructura base acumulativa de 4 clases propuesta por el **Ing. Kenji Kawaida** para la asignatura de *Programación Gráfica* (UAGRM).
* **Bibliotecas externas:**
  * **LWJGL 3.3.3 (Lightweight Java Game Library):**
    * Módulos GLFW para la creación de ventana, contexto de renderizado y captura de eventos de teclado.
    * Módulos OpenGL para bindings de funciones de **OpenGL 3.3 Core Profile**.
  * **JUnit 3.8.1 / 4:** Utilizado exclusivamente en entorno de pruebas (`src/test/java`) para validación automatizada de las físicas de colisión y límites de cuadrícula.
* **Shaders y modelos:**
  * Los shaders en lenguaje **GLSL 330 core** (Vertex Shader y Fragment Shader) fueron programados directamente en el código fuente de las clases.
  * No se utilizaron assets, modelos 3D, texturas ni librerías de sonido de terceros.
