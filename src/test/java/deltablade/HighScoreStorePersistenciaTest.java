package deltablade;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de cómo {@link HighScoreStore} guarda y lee la tabla de récords.
 * Antes de cada prueba se guardan los récords reales del jugador y después se restauran,
 * para que ejecutar los tests no borre las puntuaciones del juego.
 */
@DisplayName("Tabla de récords guardada en las preferencias")
class HighScoreStorePersistenciaTest {

    private static final String KEY_TABLE = "highScores";
    private static final String KEY_LAST_NAME = "highScoreLastName";

    private final Preferences prefs = Preferences.userNodeForPackage(HighScoreStore.class);
    private String tablaOriginal;
    private String nombreOriginal;

    @BeforeEach
    void guardarRecordsReales() {
        tablaOriginal = prefs.get(KEY_TABLE, null);
        nombreOriginal = prefs.get(KEY_LAST_NAME, null);
        prefs.remove(KEY_TABLE);
        prefs.remove(KEY_LAST_NAME);
    }

    @AfterEach
    void restaurarRecordsReales() {
        restaurar(KEY_TABLE, tablaOriginal);
        restaurar(KEY_LAST_NAME, nombreOriginal);
    }

    private void restaurar(String clave, String valor) {
        if (valor == null) {
            prefs.remove(clave);
        } else {
            prefs.put(clave, valor);
        }
    }

    @Nested
    @DisplayName("Dada una tabla vacía")
    class TablaVacia {

        @Test
        @DisplayName("Entonces no hay récords y cualquier puntuación entra")
        void noHayRecordsYCualquierPuntuacionEntra() {
            assertAll("Tabla vacía",
                    () -> assertTrue(HighScoreStore.entries().isEmpty(), "No debería haber récords"),
                    () -> assertTrue(HighScoreStore.qualifies(1), "Con la tabla vacía entra cualquier puntuación"),
                    () -> assertEquals(HighScoreStore.DEFAULT_NAME, HighScoreStore.lastName(),
                            "Sin nombre guardado se usa el nombre por defecto")
            );
        }
    }

    @Nested
    @DisplayName("Cuando se inserta un récord")
    class InsertarRecord {

        @Test
        @DisplayName("Entonces se guarda limpio y queda en primera posición")
        void elRecordSeGuardaLimpio() {
            int posicion = HighScoreStore.insert("and-rea!", 5000, 7);
            HighScoreStore.Entry entrada = HighScoreStore.entries().getFirst();
            assertAll("Primer récord",
                    () -> assertEquals(0, posicion, "Es el primer récord"),
                    () -> assertEquals("ANDREA", entrada.name(), "El nombre se limpia"),
                    () -> assertEquals(5000, entrada.score(), "Puntuación"),
                    () -> assertEquals(7, entrada.wave(), "Oleada"),
                    () -> assertEquals("ANDREA", HighScoreStore.lastName(), "Se recuerda el último nombre")
            );
        }

        @Test
        @DisplayName("Entonces una puntuación negativa se guarda como 0 y la oleada mínima es 1")
        void losValoresIncorrectosSeCorrigen() {
            HighScoreStore.insert("AAA", -50, 0);
            HighScoreStore.Entry entrada = HighScoreStore.entries().getFirst();
            assertAll("Valores corregidos",
                    () -> assertEquals(0, entrada.score(), "La puntuación nunca es negativa"),
                    () -> assertEquals(1, entrada.wave(), "La oleada mínima es 1")
            );
        }

        @Test
        @DisplayName("Entonces la tabla se ordena por puntuación y, si empatan, por oleada")
        void laTablaSeOrdena() {
            HighScoreStore.insert("BAJO", 100, 1);
            HighScoreStore.insert("ALTO", 900, 1);
            HighScoreStore.insert("EMPATE", 500, 3);
            HighScoreStore.insert("MEDIO", 500, 2);
            List<String> nombres = HighScoreStore.entries().stream().map(HighScoreStore.Entry::name).toList();
            assertEquals(List.of("ALTO", "EMPATE", "MEDIO", "BAJO"), nombres,
                    "Primero la mayor puntuación; a igual puntuación, la oleada más alta");
        }

        @Test
        @DisplayName("Entonces con la tabla llena una puntuación baja no entra")
        void conLaTablaLlenaUnaPuntuacionBajaNoEntra() {
            for (int i = 1; i <= HighScoreStore.MAX_ENTRIES; i++) {
                HighScoreStore.insert("JUGADOR", i * 1000, 1);
            }
            int posicion = HighScoreStore.insert("PEOR", 10, 1);
            assertAll("Tabla llena",
                    () -> assertEquals(-1, posicion, "No debería entrar en la tabla"),
                    () -> assertEquals(HighScoreStore.MAX_ENTRIES, HighScoreStore.entries().size(),
                            "La tabla nunca supera el máximo"),
                    () -> assertFalse(HighScoreStore.qualifies(10), "10 puntos no son suficientes")
            );
        }
    }

    @Nested
    @DisplayName("Cuando se guarda el último nombre")
    class UltimoNombre {

        @Test
        @DisplayName("Entonces un nombre sin letras se guarda como el nombre por defecto")
        void unNombreSinLetrasUsaElPorDefecto() {
            HighScoreStore.setLastName("1234");
            assertEquals(HighScoreStore.DEFAULT_NAME, HighScoreStore.lastName(),
                    "Un nombre sin letras no es válido");
        }
    }

    @Nested
    @DisplayName("Dada una tabla guardada con datos corruptos")
    class DatosCorruptos {

        @Test
        @DisplayName("Entonces se ignoran las filas que no se pueden leer")
        void seIgnoranLasFilasCorruptas() {
            prefs.put(KEY_TABLE, "AAA:100:2|basura|BBB:xx:1|CCC:50:0");
            List<HighScoreStore.Entry> tabla = HighScoreStore.entries();
            assertAll("Filas válidas",
                    () -> assertEquals(2, tabla.size(), "Solo hay dos filas válidas"),
                    () -> assertEquals("AAA", tabla.get(0).name(), "Primera fila"),
                    () -> assertEquals(1, tabla.get(1).wave(), "La oleada 0 se corrige a 1")
            );
        }

        @Test
        @DisplayName("Entonces si hay más filas que el máximo solo se leen las mejores")
        void seLimitanLasFilasAlMaximo() {
            StringBuilder datos = new StringBuilder();
            for (int i = 1; i <= HighScoreStore.MAX_ENTRIES + 5; i++) {
                if (!datos.isEmpty()) {
                    datos.append('|');
                }
                datos.append("AAA:").append(i * 10).append(":1");
            }
            prefs.put(KEY_TABLE, datos.toString());
            List<HighScoreStore.Entry> tabla = HighScoreStore.entries();
            assertAll("Tabla limitada",
                    () -> assertEquals(HighScoreStore.MAX_ENTRIES, tabla.size(), "Como máximo 10 filas"),
                    () -> assertEquals(150, tabla.getFirst().score(), "La mejor puntuación va primero")
            );
        }
    }
}
