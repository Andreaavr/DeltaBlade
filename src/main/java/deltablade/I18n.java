package deltablade;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Traducción de los textos del juego.
 * Los textos están en src/main/resources/i18n/messages_es.properties
 * y messages_en.properties.
 */
public final class I18n {

    private static final String BUNDLE = "i18n.messages";

    private static ResourceBundle bundle = load(OptionsStore.getLanguage());

    private I18n() { }

    /**
     * Devuelve el texto traducido para una clave.
     *
     * @param key clave del texto, por ejemplo "menu.start"
     * @return el texto en el idioma actual, o la propia clave si no existe
     */
    public static String tr(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Devuelve el texto traducido sustituyendo los valores indicados.
     *
     * @param key  clave del texto, por ejemplo "hud.wave"
     * @param args valores que sustituyen a %s, %d... dentro del texto
     * @return el texto en el idioma actual con los valores insertados
     */
    public static String tr(String key, Object... args) {
        return String.format(tr(key), args);
    }

    /**
     * Devuelve el idioma que se está usando.
     *
     * @return "es" o "en"
     */
    public static String language() {
        return bundle.getLocale().getLanguage();
    }

    /**
     * Cambia el idioma que se usa a partir de ahora.
     *
     * @param language "es" o "en"
     */
    public static void setLanguage(String language) {
        bundle = load(language);
    }

    private static ResourceBundle load(String language) {
        String lang = "en".equals(language) ? "en" : "es";
        return ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag(lang),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }
}