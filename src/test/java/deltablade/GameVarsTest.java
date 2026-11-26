package deltablade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias de {@link GameVars}.
 */
@DisplayName("Constantes y cálculos del juego")
class GameVarsTest {

    @ParameterizedTest(name = "Entonces en la oleada {0} el jefe es el número {1}")
    @CsvSource({
            "0, 1",
            "1, 1",
            "5, 1",
            "10, 2",
            "14, 2",
            "15, 3"
    })
    void elNumeroDeJefeDependeDeLaOleada(int oleada, int jefeEsperado) {
        assertEquals(jefeEsperado, GameVars.bossNumber(oleada),
                "Hay un jefe cada " + GameVars.BOSS_EVERY + " oleadas y como mínimo el número 1");
    }

    @Test
    @DisplayName("Entonces cada letra de EXTRA tiene su variable asociada")
    void cadaLetraExtraTieneSuVariable() {
        assertEquals(GameVars.EXTRA_LETTERS.length, GameVars.EXTRA_VARS.length,
                "Debe haber tantas variables como letras en EXTRA");
    }
}