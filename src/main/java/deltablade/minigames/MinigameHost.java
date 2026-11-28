package deltablade.minigames;

import com.almasb.fxgl.entity.Entity;
import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * Servicios que el juego principal ofrece a los minijuegos: entrada del jugador, límites del área
 * de juego, interfaz, música, efectos y el cierre del minijuego.
 */
public interface MinigameHost {

    /**
     * Termina el minijuego y vuelve al juego principal con el resultado.
     *
     * @param won {@code true} si el jugador ha ganado el minijuego
     * @param scoreBonus puntos que se suman al marcador
     * @param moneyBonus dinero que se suma al jugador
     * @param banner mensaje que se muestra al volver
     * @param color color del mensaje
     */
    void finishMinigame(boolean won, int scoreBonus, int moneyBonus, String banner, Color color);

    /**
     * Muestra un mensaje temporal en el centro de la pantalla.
     *
     * @param message texto del mensaje
     * @param color color del texto
     * @param seconds segundos que permanece visible
     */
    void displayBanner(String message, Color color, double seconds);

    /**
     * Devuelve la entidad de la nave del jugador.
     *
     * @return entidad del jugador
     */
    Entity player();

    /**
     * Indica si el jugador mantiene pulsado el botón de disparo.
     *
     * @return {@code true} si está disparando
     */
    boolean holdingFire();

    /**
     * Indica si el jugador está pulsando hacia la izquierda.
     *
     * @return {@code true} si se mueve a la izquierda
     */
    boolean movingLeft();

    /**
     * Indica si el jugador está pulsando hacia la derecha.
     *
     * @return {@code true} si se mueve a la derecha
     */
    boolean movingRight();

    /**
     * Indica si el jugador está pulsando hacia arriba.
     *
     * @return {@code true} si se mueve hacia arriba
     */
    boolean movingUp();

    /**
     * Indica si el jugador está pulsando hacia abajo.
     *
     * @return {@code true} si se mueve hacia abajo
     */
    boolean movingDown();

    /**
     * Devuelve el límite izquierdo del área de juego.
     *
     * @return coordenada X del límite izquierdo
     */
    double playLeft();

    /**
     * Devuelve el límite derecho del área de juego.
     *
     * @return coordenada X del límite derecho
     */
    double playRight();

    /**
     * Devuelve el límite superior del área de juego.
     *
     * @return coordenada Y del límite superior
     */
    double playTop();

    /**
     * Devuelve el límite inferior del área de juego.
     *
     * @return coordenada Y del límite inferior
     */
    double playBottom();

    /**
     * Añade un nodo a la capa de interfaz.
     *
     * @param node nodo de JavaFX que se añade
     */
    void addUi(Node node);

    /**
     * Quita un nodo de la capa de interfaz.
     *
     * @param node nodo de JavaFX que se quita
     */
    void removeUi(Node node);

    /**
     * Oculta o muestra la nave del jugador.
     *
     * @param hide {@code true} para ocultarla
     */
    void hidePlayer(boolean hide);

    /**
     * Sustituye la música actual por otra pista mientras dura el minijuego.
     *
     * @param fileName nombre del fichero de música
     */
    void playMusicOverride(String fileName);

    /**
     * Vuelve a poner la música que sonaba antes del minijuego.
     */
    void restoreMusic();

    /**
     * Crea una explosión en la posición indicada.
     *
     * @param x coordenada X del centro
     * @param y coordenada Y del centro
     * @param size tamaño: "hit", "ship" o "big"
     */
    void spawnExplosion(double x, double y, String size);

    /**
     * Crea una moneda en la posición indicada.
     *
     * @param x coordenada X
     * @param y coordenada Y
     * @param coinType tipo de moneda: "white", "green", "blue" o "violet"
     */
    void spawnCoinAt(double x, double y, String coinType);
}
