package deltablade.minigames;

/**
 * Minijuego que se juega dentro de la partida principal.
 */
public interface Minigame {

    /**
     * Empieza el minijuego.
     *
     * @param host juego principal que aloja el minijuego
     */
    void start(MinigameHost host);

    /**
     * Avanza el minijuego un fotograma.
     *
     * @param tpf tiempo transcurrido desde el fotograma anterior, en segundos
     */
    void update(double tpf);

    /**
     * Quita todos los elementos del minijuego de la pantalla al terminar.
     */
    void cleanup();

    /**
     * Indica si el minijuego usa la nave del jugador o la oculta.
     *
     * @return {@code true} si usa la nave del jugador
     */
    boolean usesPlayerShip();

    /**
     * Se llama cuando el jugador pulsa el botón de disparo. Por defecto no hace nada.
     */
    default void onFirePress() {}

    /**
     * Se llama cuando el jugador pulsa la tecla de bonificación. Por defecto no hace nada.
     */
    default void onBonusKey() {}
}
