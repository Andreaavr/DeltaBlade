package deltablade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de {@link HighScoreStore}.
 * No se usa insert() porque escribe en las preferencias reales del sistema.
 */
@DisplayName("Tabla de récords")
class HighScoreStoreTest {

    @Nested
    @DisplayName("Cuando el jugador escribe su nombre")
    class LimpiezaDeNombres {

        @Test
        @DisplayName("Entonces se pasa a mayúsculas y se quitan números y símbolos")
        void elNombreSePasaAMayusculasYSeQuitanSimbolos() {
            assertEquals("ANDREA", HighScoreStore.sanitizeName("and-rea!1"),
                    "Solo deberían quedar letras en mayúsculas");
        }

        @Test
        @DisplayName("Entonces se recorta a la longitud máxima permitida")
        void elNombreSeRecortaAlMaximo() {
            assertEquals(HighScoreStore.NAME_LENGTH,
                    HighScoreStore.sanitizeName("ABCDEFGHIJKLMNOP").length(),
                    "El nombre no debería superar NAME_LENGTH caracteres");
        }

        @Test
        @DisplayName("Entonces si no tiene letras se usa el nombre por defecto")
        void unNombreSinLetrasUsaElNombrePorDefecto() {
            assertAll("Nombres sin letras válidas",
                    () -> assertEquals(HighScoreStore.DEFAULT_NAME,
                            HighScoreStore.sanitizeNameOrDefault("123"),
                            "Un nombre solo con números debería sustituirse"),
                    () -> assertEquals(HighScoreStore.DEFAULT_NAME,
                            HighScoreStore.sanitizeNameOrDefault(null),
                            "Un nombre null debería sustituirse")
            );
        }
    }

    @Nested
    @DisplayName("Cuando se muestra una puntuación")
    class FormatoDePuntuacion {

        @ParameterizedTest(name = "Entonces {0} puntos se muestran como \"{1}\"")
        @CsvSource({
                "42, 000042",
                "0, 000000",
                "-10, 000000",
                "123456, 123456"
        })
        void laPuntuacionSeFormateaConSeisCifras(int puntos, String esperado) {
            assertEquals(esperado, HighScoreStore.formatScore(puntos),
                    "La puntuación debe tener 6 cifras y nunca ser negativa");
        }
    }

    @Nested
    @DisplayName("Cuando se comprueba si una puntuación entra en la tabla")
    class EntradaEnLaTabla {

        private List<HighScoreStore.Entry> tablaLlena;

        /** Crea una tabla completa cuya puntuación más baja es 100. */
        @BeforeEach
        void setUp() {
            tablaLlena = new ArrayList<>();
            for (int i = HighScoreStore.MAX_ENTRIES; i >= 1; i--) {
                tablaLlena.add(new HighScoreStore.Entry("AAA", 100 * i, 1));
            }
        }

        @Test
        @DisplayName("Dada una tabla vacía, entonces cualquier puntuación entra")
        void conLaTablaVaciaCualquierPuntuacionEntra() {
            assertAll("Tabla vacía o inexistente",
                    () -> assertTrue(HighScoreStore.qualifies(1, List.of()),
                            "Con la tabla vacía debería entrar"),
                    () -> assertTrue(HighScoreStore.qualifies(1, null),
                            "Sin tabla debería entrar")
            );
        }

        @Test
        @DisplayName("Dada una tabla llena, entonces solo entra si supera al último")
        void conLaTablaLlenaSoloEntraSiSuperaAlUltimo() {
            assertAll("Tabla llena con el último en 100 puntos",
                    () -> assertTrue(HighScoreStore.qualifies(101, tablaLlena),
                            "101 puntos supera al último y debería entrar"),
                    () -> assertFalse(HighScoreStore.qualifies(100, tablaLlena),
                            "Empatar con el último no debería bastar"),
                    () -> assertFalse(HighScoreStore.qualifies(50, tablaLlena),
                            "50 puntos no debería entrar")
            );
        }
    }
}