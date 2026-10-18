package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected float autoX = -50; // Posición horizontal inicial: centro de la calle suroeste en la ciudad ampliada.
    protected float autoZ = 50; // Posición inicial sobre el eje Z en la esquina de la ciudad ampliada.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.

    // ==================== 1.1 VARIABLES DE LA CHATA (REMOLQUE) ====================
    protected float chataX = -50; // Posición horizontal inicial de la chata.
    protected float chataZ = 53.2f; // Posición inicial en Z detrás del auto.
    protected float anguloChata = 0; // Orientación angular de la chata en radianes.
    protected static final float DISTANCIA_ENGANCHE = 1.35f; // Distancia desde el centro del auto a la bola de remolque.
    protected static final float LONGITUD_LANZA = 1.85f; // Distancia de la bola de remolque al eje central de la chata.
    protected static final float RADIO_CHATA = 1.25f; // Radio para verificar colisiones de la chata contra aceras y manzanas.

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Coloca nuevamente el auto y la chata acoplada en su punto de partida. */
    protected void reiniciar() {
        autoX = -50; // Recupera la coordenada X de inicio en la ciudad ampliada.
        autoZ = 50; // Recupera la coordenada Z de inicio en la ciudad ampliada.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
        anguloChata = 0; // Orienta la chata alineada al vehículo.
        chataX = autoX + (float) Math.sin(angulo) * (DISTANCIA_ENGANCHE + LONGITUD_LANZA); // Ubica la chata detrás del auto.
        chataZ = autoZ + (float) Math.cos(angulo) * (DISTANCIA_ENGANCHE + LONGITUD_LANZA); // Posición Z alineada detrás.
    }

    // ==================== 3. MOVIMIENTO Y FÍSICAS ACOPLADAS ====================

    /** Actualiza la conducción y la física de arrastre de la chata acoplada. */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        velocidad += acelerador * 9 * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        float resistencia = 0.7f; // Define la pérdida de velocidad normal al rodar.

        if (pulsada(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = 7; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-6, Math.min(16, velocidad)); // Limita la reversa a -6 y el avance a 16.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        // Calcula la orientación propuesta para el auto
        float siguienteAngulo = angulo + direccion * velocidad * 0.11f * deltaTime;
        float frenteX = -(float) Math.sin(siguienteAngulo); // Obtiene componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(siguienteAngulo); // Obtiene componente Z del frente del vehículo.
        float siguienteX = autoX + frenteX * velocidad * deltaTime; // Propone nueva posición X del auto.
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime; // Propone nueva posición Z del auto.

        // FÍSICA CINEMÁTICA DE LA CHATA ACOPLADA:
        // 1. Ubica el punto de enganche (bola trasera) del auto en su nueva posición propuesta
        float sigHitchX = siguienteX + (float) Math.sin(siguienteAngulo) * DISTANCIA_ENGANCHE;
        float sigHitchZ = siguienteZ + (float) Math.cos(siguienteAngulo) * DISTANCIA_ENGANCHE;

        // 2. Calcula el vector de arrastre desde el eje de la chata hacia la bola de enganche
        float dx = sigHitchX - chataX;
        float dz = sigHitchZ - chataZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        float siguienteAnguloChata = anguloChata;

        if (dist > 0.001f) {
            float anguloTiron = (float) Math.atan2(-dx, -dz); // Dirección hacia donde el enganche tira de la lanza.
            float dif = anguloTiron - anguloChata; // Desalineación angular.
            while (dif > Math.PI) dif -= 2 * Math.PI; // Normaliza el ángulo entre -PI y PI.
            while (dif < -Math.PI) dif += 2 * Math.PI;

            // La chata rota suavemente hacia la dirección de arrastre según la velocidad
            siguienteAnguloChata += dif * Math.min(1.0f, Math.abs(velocidad) * 0.45f + 0.15f) * deltaTime * 7.5f;

            // Límite físico para evitar efecto tijera (jackknife) imposible de más de 68 grados
            float difAuto = siguienteAngulo - siguienteAnguloChata;
            while (difAuto > Math.PI) difAuto -= 2 * Math.PI;
            while (difAuto < -Math.PI) difAuto += 2 * Math.PI;
            if (difAuto > 1.20f) siguienteAnguloChata = siguienteAngulo - 1.20f;
            if (difAuto < -1.20f) siguienteAnguloChata = siguienteAngulo + 1.20f;
        }

        // 3. Mantiene la distancia rígida exacta de la lanza entre el enganche y el eje de la chata
        float siguienteChataX = sigHitchX + (float) Math.sin(siguienteAnguloChata) * LONGITUD_LANZA;
        float siguienteChataZ = sigHitchZ + (float) Math.cos(siguienteAnguloChata) * LONGITUD_LANZA;

        // 4. Verificación de colisiones simultánea: tanto el auto como la chata deben tener paso libre
        if (puedeCircular(siguienteX, siguienteZ) && puedeCircularChata(siguienteChataX, siguienteChataZ)) {
            autoX = siguienteX;
            autoZ = siguienteZ;
            angulo = siguienteAngulo;
            chataX = siguienteChataX;
            chataZ = siguienteChataZ;
            anguloChata = siguienteAnguloChata;
        } else { // Si el auto o la chata chocan contra una acera/edificio, el conjunto se detiene
            velocidad = 0;
        }

        actualizarTitulo(); // Muestra los controles y la velocidad actual.
    }

    /** Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción. */
    protected void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = getClass().getSimpleName(); // Obtiene el nombre de la etapa que se está ejecutando.
        titulo += " | WASD: conducir con chata | Espacio: freno"; // Añade los controles del vehículo.
        titulo += " | C: camara | R: reiniciar | "; // Añade los controles de cámara y reinicio.
        titulo += kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /** Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni bordes. */
    protected boolean puedeCircular(float x, float z) {
        float limitePermitido = LIMITE - RADIO_AUTO; // Reserva espacio para que el auto completo quede dentro.

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == 0) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5)); // Busca el X más cercano dentro de la acera.
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5)); // Busca el Z más cercano dentro de la acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del auto al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin raíz.

                if (distanciaCuadrada < RADIO_AUTO * RADIO_AUTO) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    /** Comprueba si la chata acoplada cabe en una posición sin rozar aceras, edificios ni bordes. */
    protected boolean puedeCircularChata(float x, float z) {
        float limitePermitido = LIMITE - RADIO_CHATA;

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) {
            return false;
        }

        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 0) {
                    continue;
                }

                float centroX = centro(columna);
                float centroZ = centro(fila);
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5));
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5));
                float distanciaX = x - cercaX;
                float distanciaZ = z - cercaZ;

                if (distanciaX * distanciaX + distanciaZ * distanciaZ < RADIO_CHATA * RADIO_CHATA) {
                    return false;
                }
            }
        }

        return true;
    }

    // ==================== 5. CÁMARA ====================

    /** Elige entre una vista general y una cámara situada detrás del conjunto auto-chata. */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        // Cámara ligeramente más alejada para encuadrar perfectamente el auto y la chata acoplada
        float camaraX = autoX + (float) Math.sin(angulo) * 14.5f;
        float camaraZ = autoZ + (float) Math.cos(angulo) * 14.5f;
        vector("uOjo", camaraX, 9.8f, camaraZ); // Envía posición de la cámara en altura.
        vector("uObjetivo", autoX, 0.8f, autoZ); // Orienta la cámara hacia el vehículo.
        decimal("uAspecto", (float) ancho / alto); // Mantiene proporciones.
    }

    // ==================== 6. DIBUJO DEL AUTO Y LA CHATA ====================

    /** Dibuja primero la ciudad, luego el vehículo y la chata acoplada. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
        dibujarChata(); // Añade la chata (remolque acoplado con lanza, madera y ruedas).
    }

    /** Construye el auto con cajas; argumentos: posición XYZ, tamaño XYZ y color RGB. */
    protected void dibujarAuto() {
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f); // Carrocería roja.
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Cabina azulada.
        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = {-0.82f, 0.82f}; // Ubica las ruedas delanteras y traseras.

        for (float x : ladosRuedas) {
            for (float z : ejesRuedas) {
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Rueda oscura.
            }
        }

        float[] ladosFaros = {-0.55f, 0.55f}; // Define separación lateral de las luces.
        for (float x : ladosFaros) {
            pieza(x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Faro delantero claro.
            pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Luz trasera roja.
        }

        // Enganche de remolque en la parte trasera del auto (soporte negro y bola de acople)
        pieza(0, 0.40f, 1.36f, 0.14f, 0.12f, 0.22f, 0.15f, 0.16f, 0.18f); // Brazo de enganche
        pieza(0, 0.45f, 1.45f, 0.12f, 0.12f, 0.12f, 0.75f, 0.78f, 0.82f); // Bola esférica de acople plateada
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación del auto.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        float mundoX = autoX + coseno * x + seno * z; // Gira la posición local y suma la posición X del auto.
        float mundoZ = autoZ - seno * x + coseno * z; // Gira la posición local y suma la posición Z del auto.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Transforma una pieza del espacio local de la chata al espacio de la ciudad. */
    protected void piezaChata(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(anguloChata); // Calcula coseno del ángulo de la chata.
        float seno = (float) Math.sin(anguloChata); // Calcula seno del ángulo de la chata.
        float mundoX = chataX + coseno * x + seno * z; // Gira y traslada en X de la chata.
        float mundoZ = chataZ - seno * x + coseno * z; // Gira y traslada en Z de la chata.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, anguloChata); // Dibuja la pieza orientada según la chata.
    }

    /** Construye el modelo tridimensional de la chata (elementos esenciales: lanza, piso, barandas y ruedas). */
    protected void dibujarChata() {
        // 1. Lanza de enganche: barra de acero que conecta con el auto
        piezaChata(0, 0.40f, -1.25f, 0.14f, 0.10f, 1.20f, 0.15f, 0.16f, 0.18f);

        // 2. Piso de carga: base rectangular de madera
        piezaChata(0, 0.38f, 0, 1.40f, 0.10f, 1.70f, 0.58f, 0.38f, 0.20f);

        // 3. Barandas laterales: paredes izquierda y derecha de madera
        for (float x : new float[]{-0.66f, 0.66f}) {
            piezaChata(x, 0.60f, 0, 0.08f, 0.36f, 1.70f, 0.68f, 0.43f, 0.22f);
        }

        // 4. Baranda delantera y compuerta trasera de madera
        for (float z : new float[]{-0.81f, 0.81f}) {
            piezaChata(0, 0.60f, z, 1.40f, 0.36f, 0.08f, 0.68f, 0.43f, 0.22f);
        }

        // 5. Dos ruedas oscuras a los costados
        for (float x : new float[]{-0.84f, 0.84f}) {
            piezaChata(x, 0.38f, 0, 0.22f, 0.54f, 0.56f, 0.055f, 0.065f, 0.08f);
        }
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        clase2 aplicacion = new clase2(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
