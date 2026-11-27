package deltablade;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pruebas unitarias de {@link I18n} y de los ficheros de idioma.
 */
@DisplayName("Traducción de los textos del juego")
class I18nTest {

    @AfterEach
    void restaurarIdioma() {
        I18n.setLanguage(OptionsStore.getLanguage());
    }

    private static Set<String> claves(String idioma) {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag(idioma),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        return new TreeSet<>(bundle.keySet());
    }

    @Nested
    @DisplayName("Dados los ficheros de idioma")
    class FicherosDeIdioma {

        @Test
        @DisplayName("Entonces español e inglés tienen exactamente las mismas claves")
        void ambosIdiomasTienenLasMismasClaves() {
            Set<String> soloEspanol = claves("es");
            soloEspanol.removeAll(claves("en"));
            Set<String> soloIngles = claves("en");
            soloIngles.removeAll(claves("es"));
            assertAll("Claves de messages_es y messages_en",
                    () -> assertEquals(Set.of(), soloEspanol, "Claves que solo están en español"),
                    () -> assertEquals(Set.of(), soloIngles, "Claves que solo están en inglés")
            );
        }
    }

    @Nested
    @DisplayName("Cuando se cambia de idioma")
    class CambioDeIdioma {

        @Test
        @DisplayName("Entonces el mismo texto sale en español o en inglés")
        void elTextoCambiaSegunElIdioma() {
            I18n.setLanguage("es");
            String espanol = I18n.tr("menu.start");
            I18n.setLanguage("en");
            String ingles = I18n.tr("menu.start");
            assertAll("menu.start",
                    () -> assertEquals("JUGAR", espanol, "En español"),
                    () -> assertEquals("START GAME", ingles, "En inglés")
            );
        }

        @ParameterizedTest(name = "Entonces el idioma \"{0}\" se trata como español")
        @ValueSource(strings = {"fr", "de", ""})
        void unIdiomaDesconocidoUsaEspanol(String idioma) {
            I18n.setLanguage(idioma);
            assertEquals("es", I18n.language(), "Solo hay español e inglés; el resto usa español");
        }
    }

    @Nested
    @DisplayName("Cuando se pide un texto")
    class PeticionDeTexto {

        @Test
        @DisplayName("Entonces una clave que no existe devuelve la propia clave")
        void unaClaveInexistenteDevuelveLaClave() {
            assertEquals("clave.que.no.existe", I18n.tr("clave.que.no.existe"),
                    "Si falta una traducción, el juego no debe romperse");
        }

        @Test
        @DisplayName("Entonces los valores se insertan en el texto")
        void losValoresSeInsertanEnElTexto() {
            I18n.setLanguage("es");
            assertAll("Textos con valores",
                    () -> assertEquals("OLEADA 3", I18n.tr("wave.normal", 3), "Número de oleada"),
                    () -> assertEquals("PREC 75%", I18n.tr("hud.accuracy", 75), "Porcentaje de precisión")
            );
        }

        @Test
        @DisplayName("Entonces ningún texto está vacío")
        void ningunTextoEstaVacio() {
            for (String idioma : new String[] {"es", "en"}) {
                I18n.setLanguage(idioma);
                for (String clave : claves(idioma)) {
                    assertFalse(I18n.tr(clave).isBlank(), "La clave " + clave + " está vacía en " + idioma);
                }
            }
        }
    }
}
