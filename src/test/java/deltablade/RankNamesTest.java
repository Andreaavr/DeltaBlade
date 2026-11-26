package deltablade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pruebas unitarias de {@link RankNames}.
 */
@DisplayName("Nombres de rango del jugador")
class RankNamesTest {

    @Nested
    @DisplayName("Dado un rango válido")
    class RangoValido {

        @Test
        @DisplayName("Entonces el rango 0 es RECRUIT")
        void elPrimerRangoEsRecluta() {
            assertEquals("RECRUIT", RankNames.title(0),
                    "El primer rango del juego debería ser RECRUIT");
        }

        @Test
        @DisplayName("Entonces el banner muestra 'RANK' seguido del título")
        void elBannerIncluyeElTitulo() {
            assertEquals("RANK  RECRUIT", RankNames.banner(0),
                    "El banner debe tener el formato 'RANK  <título>'");
        }
    }

    @Nested
    @DisplayName("Dado un rango fuera de lo normal")
    class RangoFueraDeRango {

        @ParameterizedTest(name = "Entonces el rango {0} se trata como el rango 0")
        @ValueSource(ints = {-1, -5, -100})
        void unRangoNegativoSeTrataComoCero(int rangoNegativo) {
            assertEquals(RankNames.title(0), RankNames.title(rangoNegativo),
                    "Un rango negativo debería mostrarse como el primer rango");
        }

        @Test
        @DisplayName("Entonces un rango muy alto devuelve un título no vacío")
        void losRangosAltosNoDevuelvenTextoVacio() {
            String titulo = RankNames.title(1000);
            assertAll("Título para el rango 1000",
                    () -> assertNotNull(titulo, "El título no debería ser null"),
                    () -> assertFalse(titulo.isBlank(), "El título no debería estar vacío")
            );
        }
    }
}