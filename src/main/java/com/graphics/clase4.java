package com.graphics; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
 * Hereda clase3, que hereda clase2, que a su vez hereda clase1.
 * Los semáforos son decorativos; el ejemplo no incluye tráfico autónomo.
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    private boolean pausado = false; // Permite pausar/reanudar la simulación con la tecla P.
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    private static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle de la ciudad ampliada.
        {50, -50},  // Primera entrega: esquina noreste de la ciudad ampliada.
        {-50, -50}, // Segunda entrega: esquina noroeste de la ciudad ampliada.
        {10, 10},   // Tercera entrega: sector comercial central.
        {-50, 50}   // Cuarta entrega: regreso a la parada de inicio suroeste.
    };

    // ==================== 1.1 TRÁFICO AUTÓNOMO (MEJORA OPCIONAL) ====================
    /** Estructura que gestiona un vehículo autónomo que circula por las calles. */
    private static class AutoTrafico {
        float x, z; // Posición actual sobre la calle.
        float angulo; // Orientación hacia la que avanza el auto.
        float velocidad; // Rapidez de desplazamiento constante.
        float r, g, b; // Color distintivo del vehículo.
        boolean esTaxi; // Identifica si lleva letrero en el techo.
        float[][] ruta; // Secuencia de intersecciones que forman su circuito.
        int puntoActual; // Siguiente destino hacia el que se desplaza.

        AutoTrafico(float[][] ruta, float vel, float r, float g, float b, boolean esTaxi) {
            this.ruta = ruta; // Asigna los puntos de control de la calle.
            this.x = ruta[0][0]; // Inicia en el primer punto de su ruta.
            this.z = ruta[0][1]; // Inicia en la coordenada Z correspondiente.
            this.velocidad = vel; // Define la velocidad del vehículo.
            this.r = r; // Asigna componente roja.
            this.g = g; // Asigna componente verde.
            this.b = b; // Asigna componente azul.
            this.esTaxi = esTaxi; // Configura si es taxi.
            this.puntoActual = 1; // Fija el siguiente punto de la ruta.
            actualizarGiro(); // Calcula el ángulo inicial.
        }

        void actualizar(float dt) {
            float destinoX = ruta[puntoActual][0]; // X del siguiente cruce.
            float destinoZ = ruta[puntoActual][1]; // Z del siguiente cruce.
            float dx = destinoX - x; // Distancia horizontal pendiente.
            float dz = destinoZ - z; // Distancia en profundidad pendiente.
            float distancia = (float) Math.sqrt(dx * dx + dz * dz); // Distancia euclidiana al cruce.
            if (distancia < 0.8f) { // Detecta que alcanzó la intersección.
                puntoActual = (puntoActual + 1) % ruta.length; // Pasa al siguiente cruce en circuito cerrado.
                actualizarGiro(); // Orienta el auto hacia la nueva dirección.
            } else {
                float dirX = dx / distancia; // Vector unitario en X.
                float dirZ = dz / distancia; // Vector unitario en Z.
                x += dirX * velocidad * dt; // Desplaza el vehículo en X.
                z += dirZ * velocidad * dt; // Desplaza el vehículo en Z.
                angulo = (float) Math.atan2(-dirX, -dirZ); // Asegura que el auto apunte al frente de avance.
            }
        }

        void actualizarGiro() {
            float dx = ruta[puntoActual][0] - x; // Dirección horizontal hacia el destino.
            float dz = ruta[puntoActual][1] - z; // Dirección en profundidad.
            angulo = (float) Math.atan2(-dx, -dz); // Calcula el ángulo en radianes.
        }

        void reiniciar() {
            x = ruta[0][0]; // Restablece posición inicial en X.
            z = ruta[0][1]; // Restablece posición inicial en Z.
            puntoActual = 1; // Restaura el primer tramo del circuito.
            actualizarGiro(); // Reestablece la orientación inicial.
        }
    }

    // Tres vehículos autónomos con circuitos por las calles de la ciudad ampliada:
    private final AutoTrafico[] trafico = {
        // Vehículo 1: Taxi amarillo circulando por el circuito noreste.
        new AutoTrafico(new float[][]{{10, -50}, {50, -50}, {50, -10}, {10, -10}}, 7.5f, 0.98f, 0.82f, 0.10f, true),
        // Vehículo 2: Patrulla azul circulando por el circuito noroeste/central.
        new AutoTrafico(new float[][]{{-50, -30}, {-10, -30}, {-10, 10}, {-50, 10}}, 8.0f, 0.15f, 0.40f, 0.92f, false),
        // Vehículo 3: Auto verde circulando por el circuito sur.
        new AutoTrafico(new float[][]{{-30, 30}, {30, 30}, {30, 50}, {-30, 50}}, 7.0f, 0.18f, 0.78f, 0.42f, false)
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa y la pausa a los controles anteriores. */
    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
        if (key == GLFW_KEY_P) { // Comprueba la tecla de pausa de la simulación.
            pausado = !pausado; // Alterna el estado de pausa del juego.
        }
    }

    /** Reinicia el vehículo, el progreso de las entregas y el tráfico autónomo. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo del jugador.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        pausado = false; // Quita la pausa si estaba activa.
        for (AutoTrafico auto : trafico) { // Recorre los vehículos autónomos.
            auto.reiniciar(); // Regresa cada vehículo al inicio de su circuito.
        }
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo, los autos de tráfico y comprueba paradas. */
    @Override // Añade el objetivo del juego y tráfico al movimiento heredado.
    protected void actualizar(float deltaTime) {
        if (pausado) { // Comprueba si el usuario pausó la simulación.
            actualizarTitulo(); // Actualiza el título indicando la pausa.
            return; // No avanza físicas, movimiento ni cronómetro mientras esté en pausa.
        }
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        for (AutoTrafico auto : trafico) { // Actualiza cada vehículo autónomo por separado.
            auto.actualizar(deltaTime); // Mueve el auto siguiendo su circuito vial.
        }
        if (entregas >= DESTINOS.length) { // Comprueba si ya se completaron todas las paradas.
            return; // Conserva el tiempo final y evita leer fuera del arreglo.
        }
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = autoX - DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = autoZ - DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < 3 * 3; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(velocidad) < 1; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /** Compone el progreso que se añade al título de la ventana. */
    @Override // Amplía los indicadores de día/noche y faros de clase3.
    protected String estadoExtra() {
        if (pausado) { // Si está en pausa, muestra aviso destacado.
            return " | [PAUSA: Pulsa P para reanudar]";
        }
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa | P: pausa | "; // Muestra las teclas de minimapa y pausa.
        if (entregas == DESTINOS.length) { // Selecciona el texto de victoria al completar las paradas.
            mensaje += "GANASTE en " + (int) tiempo + " s! R: reiniciar"; // Muestra tiempo final y opción de reinicio.
        } else { // Durante el recorrido muestra progreso e instrucciones.
            mensaje += "Entregas " + entregas + "/" + DESTINOS.length; // Indica cuántas paradas se completaron.
            mensaje += " | Frena en marca dorada | " + (int) tiempo + " s"; // Explica la condición de entrega y el tiempo.
        }
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración, tráfico autónomo y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas, semáforos y tótems de sectores.
        }
        // Dibuja los vehículos de tráfico autónomo en la ciudad
        for (AutoTrafico auto : trafico) {
            dibujarVehiculoTrafico(auto); // Dibuja cada vehículo en su posición y ángulo actual.
        }
        if (entregas < DESTINOS.length) { // Dibuja un objetivo únicamente mientras queden entregas.
            dibujarDestino(); // Coloca la marca dorada en la parada activa.
        }
    }

    /** Dibuja un vehículo autónomo con carrocería, cabina, ruedas, luces y accesorios. */
    private void dibujarVehiculoTrafico(AutoTrafico auto) {
        float cos = (float) Math.cos(auto.angulo); // Coseno de la rotación del auto.
        float sen = (float) Math.sin(auto.angulo); // Seno de la rotación del auto.

        // Carrocería principal con el color característico del vehículo
        cajaGirada(auto.x, 0.65f, auto.z, 1.6f, 0.55f, 2.5f, auto.r, auto.g, auto.b, auto.angulo);
        // Cabina con tono de cristal tintado
        float cabinaX = auto.x + sen * 0.12f;
        float cabinaZ = auto.z + cos * 0.12f;
        cajaGirada(cabinaX, 1.12f, cabinaZ, 1.25f, 0.52f, 1.2f, 0.22f, 0.65f, 0.78f, auto.angulo);

        // Cuatro ruedas en sus respectivas posiciones relativas
        float[] ladosRuedas = {-0.86f, 0.86f};
        float[] ejesRuedas = {-0.78f, 0.78f};
        for (float rx : ladosRuedas) {
            for (float rz : ejesRuedas) {
                float ruedaX = auto.x + cos * rx + sen * rz;
                float ruedaZ = auto.z - sen * rx + cos * rz;
                cajaGirada(ruedaX, 0.38f, ruedaZ, 0.24f, 0.55f, 0.58f, 0.06f, 0.07f, 0.08f, auto.angulo);
            }
        }

        // Faros frontales y traseros
        float frenteX = -(float) Math.sin(auto.angulo);
        float frenteZ = -(float) Math.cos(auto.angulo);
        float lateralX = -(float) Math.cos(auto.angulo);
        float lateralZ = (float) Math.sin(auto.angulo);

        for (float sep : new float[]{-0.52f, 0.52f}) {
            float fx = auto.x + frenteX * 1.28f + lateralX * sep;
            float fz = auto.z + frenteZ * 1.28f + lateralZ * sep;
            if (noche) {
                entero("uEmision", 1); // Hace brillar los faros en la noche.
            }
            cajaGirada(fx, 0.68f, fz, 0.35f, 0.18f, 0.06f, 1.0f, 0.95f, 0.65f, auto.angulo); // Faro delantero.
            entero("uEmision", 0);

            float tx = auto.x - frenteX * 1.28f + lateralX * sep;
            float tz = auto.z - frenteZ * 1.28f + lateralZ * sep;
            cajaGirada(tx, 0.68f, tz, 0.32f, 0.16f, 0.06f, 0.85f, 0.06f, 0.06f, auto.angulo); // Faro trasero rojo.
        }

        // Letrero luminoso superior si es taxi
        if (auto.esTaxi) {
            if (noche) {
                entero("uEmision", 1);
            }
            cajaGirada(cabinaX, 1.45f, cabinaZ, 0.6f, 0.18f, 0.35f, 1.0f, 0.88f, 0.18f, auto.angulo);
            entero("uEmision", 0);
        }
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Dibuja una marca dorada sobre el asfalto.
        if (!vistaMapa) { // Evita añadir una baliza tridimensional al mapa pequeño.
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            cajaGirada(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, tiempo); // Dibuja el cubo giratorio del objetivo.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                if (tipo == 2) { // Detecta una parcela de parque.
                    dibujarParque(x, z); // Añade árboles y un banco.
                }
                if (tipo == 1) { // Detecta una parcela con edificio.
                    float altura = 5 + (fila * 3 + columna * 7) % 9; // Recupera la misma altura calculada en clase1.
                    dibujarVentanas(x, z, altura); // Coloca ventanas en sus cuatro fachadas.
                }
                if (tipo != 0) { // La señalización se coloca junto a las manzanas, no en celdas de calle.
                    dibujarPasoPeatonal(x, z); // Añade el cruce pintado sobre la calle contigua.
                    dibujarSemaforo(x + 4, z - 4); // Coloca el semáforo dentro de la acera.
                }
            }
        }
        dibujarSenalizacionSectores(); // Añade tótems de orientación con nombres de cuadrantes.
    }

    /** Distribuye tótems representativos de sectores urbanos en las avenidas cardinales. */
    private void dibujarSenalizacionSectores() {
        dibujarTotem(0, -48f, 0.22f, 0.50f, 0.88f);  // Tótem Avenida Norte (Azul cívico).
        dibujarTotem(0, 48f, 0.20f, 0.75f, 0.38f);   // Tótem Boulevard Sur (Verde parque).
        dibujarTotem(48f, 0, 0.95f, 0.65f, 0.15f);   // Tótem Distrito Este (Dorado comercial).
        dibujarTotem(-48f, 0, 0.85f, 0.28f, 0.28f);  // Tótem Sector Oeste (Rojo residencial).
    }

    /** Construye un tótem urbano con zócalo, poste y cartel reflectante. */
    private void dibujarTotem(float x, float z, float r, float g, float b) {
        caja(x, 0.25f, z, 1.4f, 0.5f, 1.4f, 0.30f, 0.32f, 0.35f); // Base de hormigón.
        caja(x, 1.8f, z, 0.28f, 3.2f, 0.28f, 0.15f, 0.18f, 0.22f); // Columna metálica oscura.
        if (noche) {
            entero("uEmision", 1); // Hace visible el cartel de noche.
        }
        caja(x, 3.6f, z, 2.4f, 1.0f, 0.35f, r, g, b); // Panel indicador del sector.
        caja(x, 3.6f, z, 2.5f, 0.15f, 0.40f, 1.0f, 1.0f, 1.0f); // Franja reflectante blanca.
        entero("uEmision", 0);
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) { // Las ventanas simulan habitaciones encendidas en el ambiente nocturno.
            entero("uEmision", 1); // Permite ver el color de las ventanas sin depender de farolas.
        }
        for (float y = 1.7f; y < altura; y += 2) { // Recorre los pisos separados por dos unidades de altura.
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) { // Coloca tres ventanas por fachada.
                caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada norte.
                caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada sur.
                caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada oeste.
                caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada este.
            }
        }
        entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /** Dibuja las franjas blancas sobre la calle contigua al norte de la manzana. */
    private void dibujarPasoPeatonal(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) { // Coloca siete franjas paralelas.
            caja(x + desplazamiento, 0.045f, z - 8, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Eleva la pintura un poco sobre el suelo.
        }
    }

    /** Construye un semáforo decorativo que alterna rojo, verde y amarillo cada 12 segundos. */
    private void dibujarSemaforo(float x, float z) {
        caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa de las tres luces.
        int fase = (int) (tiempo % 12); // Repite un ciclo de segundos comprendidos entre 0 y 11.
        for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
            boolean encendida = false; // Parte de una bombilla apagada.
            if (indice == 0) { // Selecciona la bombilla roja.
                encendida = fase < 5; // Mantiene rojo durante los primeros cinco segundos.
            } else if (indice == 1) { // Selecciona la bombilla amarilla.
                encendida = fase >= 10; // Mantiene amarillo en los últimos dos segundos del ciclo.
            } else { // Selecciona la bombilla verde.
                encendida = fase >= 5 && fase < 10; // Mantiene verde durante los cinco segundos intermedios.
            }
            float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
            entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
            if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                brillo = 1; // Usa intensidad completa para su color.
                entero("uEmision", 1); // Hace que la bombilla se vea encendida.
            }
            float rojo = 0; // Componente roja inicialmente ausente.
            float verde = 0; // Componente verde inicialmente ausente.
            if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                rojo = brillo; // Añade rojo con la intensidad elegida.
            }
            if (indice > 0) { // Amarillo y verde necesitan componente verde.
                verde = brillo; // Añade verde; rojo más verde produce amarillo.
            }
            float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
            caja(x, altura, z - 0.24f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Dibuja la bombilla frente a la carcasa.
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        if (!mostrarMapa) { // Comprueba si el usuario ocultó el minimapa con M.
            return; // Conserva únicamente la imagen principal.
        }
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(260, dimensionMenor / 3); // Limita el mapa a 260 píxeles y a un tercio de la ventana.
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
        }
    }

    /** Dibuja el indicador del auto y el tráfico sobre los edificios en el minimapa. */
    private void dibujarIndicadorAuto() {
        // Muestra los vehículos de tráfico autónomo en el minimapa con sus respectivos colores
        for (AutoTrafico auto : trafico) {
            cajaGirada(auto.x, 24, auto.z, 2.0f, 0.1f, 2.8f, auto.r, auto.g, auto.b, auto.angulo);
        }

        // Indicador de la chata acoplada en el minimapa (marrón madera con enganche)
        cajaGirada(chataX, 24.5f, chataZ, 2.0f, 0.1f, 2.3f, 0.68f, 0.43f, 0.22f, anguloChata);

        // Indicador del vehículo del jugador (resaltado en cian y blanco)
        cajaGirada(autoX, 25, autoZ, 2.6f, 0.1f, 3.6f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 2.2f; // Desplaza la punta hacia delante en X.
        float puntaZ = autoZ + frenteZ * 2.2f; // Desplaza la punta hacia delante en Z.
        caja(puntaX, 26, puntaZ, 1.1f, 0.1f, 1.1f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
