package deltablade;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAudioPlayer;
import static com.almasb.fxgl.dsl.FXGL.getSettings;

/**
 * Música de fondo: repite en bucle la canción elegida del catálogo y respeta el encendido/apagado y el volumen.
 * Las canciones del jugador están en la carpeta de música del sistema ({@code DeltaBlade});
 * los ficheros incluidos con el juego se copian ahí al principio.
 */
public final class MusicHelper {

    /**
     * Pista de música disponible en el juego.
     *
     * @param id identificador único de la pista
     * @param fileName nombre del fichero de audio
     * @param displayName nombre que se muestra en el menú de opciones
     * @param sourceUrl dirección de la que procede la pista, si se conoce
     */
    public record Track(String id, String fileName, String displayName, String sourceUrl) {}

    private static List<Track> catalog;
    private static MediaPlayer player;
    private static String currentTrackId;
    private static boolean playing;
    private static boolean loggedMissing;

    private MusicHelper() {}

    /**
     * Devuelve las pistas de música disponibles.
     *
     * @return lista de pistas
     */
    public static List<Track> tracks() {
        return catalog();
    }

    /**
     * Vuelve a buscar las pistas de música en las carpetas de música.
     */
    public static void rescan() {
        catalog = MusicCatalog.scan();
    }

    /**
     * Busca una pista por su identificador.
     *
     * @param id identificador de la pista
     * @return la pista encontrada, o la primera disponible si no existe
     */
    public static Track find(String id) {
        return MusicCatalog.lookup(catalog(), id).track();
    }

    /**
     * Devuelve la pista que está sonando o, si no suena ninguna, la elegida en las opciones.
     *
     * @return pista actual
     */
    public static Track currentTrack() {
        return find(currentTrackId != null ? currentTrackId : OptionsStore.getSelectedTrackId());
    }

    /**
     * Devuelve la posición de una pista en la lista de pistas disponibles.
     *
     * @param id identificador de la pista
     * @return índice de la pista, o 0 si no se encuentra
     */
    public static int indexOf(String id) {
        return Math.max(0, MusicCatalog.lookup(catalog(), id).index());
    }

    /**
     * Aplica las opciones guardadas: el volumen siempre, y arranca o para el bucle según haga falta.
     */
    public static void applyFromStore() {
        setVolume(OptionsStore.getMusicVolume());
        if (!OptionsStore.isMusicEnabled()) {
            stop();
            return;
        }
        Track track = find(OptionsStore.getSelectedTrackId());
        if (track == null) {
            stop();
            return;
        }
        play(track);
    }

    /**
     * Cambia el volumen de la música.
     *
     * @param volume volumen entre 0.0 y 1.0 (los valores fuera de rango se ajustan)
     */
    public static void setVolume(double volume) {
        double clamped = Math.max(0.0, Math.min(1.0, volume));
        try {
            getSettings().setGlobalMusicVolume(clamped);
        } catch (Exception ignored) {
        }
        if (player != null) {
            player.setVolume(clamped);
        }
        JavaSoundMusic.setVolume(clamped);
    }

    /**
     * Reproduce una pista concreta en lugar de la elegida en las opciones, por ejemplo durante un minijuego.
     *
     * @param fileName nombre del fichero de música
     */
    public static void playOverride(String fileName) {
        setVolume(OptionsStore.getMusicVolume());
        if (!OptionsStore.isMusicEnabled()) {
            stop();
            return;
        }
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        URL resource = MusicHelper.class.getResource("/assets/music/" + fileName);
        if (resource == null) {
            logMissing(fileName);
            applyFromStore();
            return;
        }
        playUrl(resource.toExternalForm(), "override:" + fileName);
    }

    /**
     * Reproduce una pista en bucle. Si ya está sonando, solo actualiza el volumen.
     *
     * @param track pista que se reproduce; si es {@code null} no hace nada
     */
    public static void play(Track track) {
        if (track == null) {
            return;
        }
        if (playing && track.id().equals(currentTrackId)) {
            setVolume(OptionsStore.getMusicVolume());
            return;
        }
        String url = track.sourceUrl();
        if (url == null || url.isBlank()) {
            URL resource = MusicHelper.class.getResource("/assets/music/" + track.fileName());
            if (resource == null) {
                logMissing(track.fileName());
                return;
            }
            url = resource.toExternalForm();
        }
        playUrl(url, track.id());
    }

    /**
     * Detiene la música que esté sonando.
     */
    public static void stop() {
        JavaSoundMusic.stop();
        try {
            if (player != null) {
                player.stop();
                player.dispose();
            }
        } catch (Exception ignored) {
        }
        player = null;
        try {
            getAudioPlayer().stopAllMusic();
        } catch (Exception ignored) {
        }
        currentTrackId = null;
        playing = false;
    }

    /**
     * Indica si hay música sonando.
     *
     * @return {@code true} si suena alguna pista
     */
    public static boolean isPlaying() {
        return playing || JavaSoundMusic.isPlaying();
    }

    private static boolean preferJavaSound() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux");
    }

    private static void playUrl(String url, String trackId) {
        stop();
        if (preferJavaSound() && JavaSoundMusic.looksSupported(url)) {
            JavaSoundMusic.setVolume(OptionsStore.getMusicVolume());
            JavaSoundMusic.playLoop(url);
            currentTrackId = trackId;
            playing = true;
            return;
        }
        try {
            Media media = new Media(url);
            MediaPlayer next = new MediaPlayer(media);
            next.setCycleCount(MediaPlayer.INDEFINITE);
            next.setVolume(OptionsStore.getMusicVolume());
            next.setOnError(() -> {
                if (JavaSoundMusic.looksSupported(url)) {
                    JavaSoundMusic.setVolume(OptionsStore.getMusicVolume());
                    JavaSoundMusic.playLoop(url);
                    currentTrackId = trackId;
                    playing = true;
                    return;
                }
                logMissing(url + " - " + String.valueOf(next.getError()));
                if (trackId != null && trackId.startsWith("override:")) {
                    applyFromStore();
                }
            });
            next.play();
            player = next;
            currentTrackId = trackId;
            playing = true;
        } catch (Exception e) {
            if (JavaSoundMusic.looksSupported(url)) {
                JavaSoundMusic.setVolume(OptionsStore.getMusicVolume());
                JavaSoundMusic.playLoop(url);
                currentTrackId = trackId;
                playing = true;
                return;
            }
            logMissing(url + " - " + e.getMessage());
        }
    }

    private static List<Track> catalog() {
        if (catalog == null) {
            catalog = MusicCatalog.scan();
        }
        return catalog;
    }

    private static void logMissing(String detail) {
        if (loggedMissing) {
            return;
        }
        loggedMissing = true;
        System.err.println("[MusicHelper] Music not available: " + detail);
    }
}
