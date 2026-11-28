package deltablade;

import java.util.prefs.Preferences;

/**
 * Opciones del jugador que se conservan al reiniciar gracias a Java Preferences.
 */
public final class OptionsStore {

    private static final Preferences PREFS = Preferences.userNodeForPackage(OptionsStore.class);

    private static final String KEY_MUSIC_ENABLED = "musicEnabled";
    private static final String KEY_MUSIC_VOLUME = "musicVolume";
    private static final String KEY_EFFECT_VOLUME = "effectVolume";
    private static final String KEY_MUSIC_TRACK = "musicTrack";
    private static final String KEY_LANGUAGE = "language";

    /** Idioma por defecto del juego. */
    public static final String DEFAULT_LANGUAGE = "es";

    public static final double DEFAULT_MUSIC_VOLUME = 0.55;
    public static final double DEFAULT_EFFECT_VOLUME = 0.55;

    private OptionsStore() {}

    /**
     * Indica si la música está activada.
     *
     * @return {@code true} si la música está activada (por defecto sí)
     */
    public static boolean isMusicEnabled() {
        return PREFS.getBoolean(KEY_MUSIC_ENABLED, true);
    }

    /**
     * Activa o desactiva la música.
     *
     * @param enabled {@code true} para activarla
     */
    public static void setMusicEnabled(boolean enabled) {
        PREFS.putBoolean(KEY_MUSIC_ENABLED, enabled);
    }

    /**
     * Devuelve el volumen guardado de la música.
     *
     * @return volumen entre 0.0 y 1.0
     */
    public static double getMusicVolume() {
        return clamp01(PREFS.getDouble(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME));
    }

    /**
     * Guarda el volumen de la música.
     *
     * @param volume volumen entre 0.0 y 1.0 (los valores fuera de rango se ajustan)
     */
    public static void setMusicVolume(double volume) {
        PREFS.putDouble(KEY_MUSIC_VOLUME, clamp01(volume));
    }

    /**
     * Devuelve el volumen guardado de los efectos de sonido.
     *
     * @return volumen entre 0.0 y 1.0
     */
    public static double getEffectVolume() {
        return clamp01(PREFS.getDouble(KEY_EFFECT_VOLUME, DEFAULT_EFFECT_VOLUME));
    }

    /**
     * Guarda el volumen de los efectos de sonido.
     *
     * @param volume volumen entre 0.0 y 1.0 (los valores fuera de rango se ajustan)
     */
    public static void setEffectVolume(double volume) {
        PREFS.putDouble(KEY_EFFECT_VOLUME, clamp01(volume));
    }

    /**
     * Devuelve el identificador de la pista de música elegida.
     *
     * @return identificador de la pista, o una cadena vacía si no se ha elegido ninguna
     */
    public static String getSelectedTrackId() {
        return PREFS.get(KEY_MUSIC_TRACK, "");
    }

    /**
     * Guarda la pista de música elegida. Los identificadores vacíos se ignoran.
     *
     * @param trackId identificador de la pista
     */
    public static void setSelectedTrackId(String trackId) {
        if (trackId == null || trackId.isBlank()) {
            return;
        }
        PREFS.put(KEY_MUSIC_TRACK, trackId);
    }


    /**
     * Devuelve el idioma elegido por el jugador.
     *
     * @return "es" para español o "en" para inglés
     */
    public static String getLanguage() {
        return "en".equals(PREFS.get(KEY_LANGUAGE, DEFAULT_LANGUAGE)) ? "en" : "es";
    }

    /**
     * Guarda el idioma elegido por el jugador.
     *
     * @param language "es" o "en"; cualquier otro valor se guarda como "es"
     */
    public static void setLanguage(String language) {
        PREFS.put(KEY_LANGUAGE, "en".equals(language) ? "en" : "es");
    }

    /**
     * Devuelve el récord de supervivencia del minijuego Kobarmaru Yashi.
     *
     * @return récord en segundos
     */
    public static int getAlienRecordSeconds() {
        return PREFS.getInt("alienEnduranceRecord", 0);
    }

    /**
     * Guarda el récord de supervivencia del minijuego Kobarmaru Yashi.
     *
     * @param seconds récord en segundos (los negativos se guardan como 0)
     */
    public static void setAlienRecordSeconds(int seconds) {
        PREFS.putInt("alienEnduranceRecord", Math.max(0, seconds));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
