package deltablade;

/**
 * Punto de entrada del JAR ejecutable.
 * Es necesario porque la clase principal del juego hereda de
 * javafx.application.Application y no puede lanzarse directamente
 * desde un JAR con todas las dependencias incluidas.
 */
public final class Launcher {

    private Launcher() { }

    /**
     * Arranca el juego.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        DeltaBladeApp.main(args);
    }
}