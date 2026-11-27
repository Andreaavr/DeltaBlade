package deltablade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de {@link ShopWeapon}.
 */
@DisplayName("Armas especiales de la tienda")
class ShopWeaponTest {

    @Nested
    @DisplayName("Cuando se busca un arma por su identificador")
    class BusquedaPorId {

        @Test
        @DisplayName("Entonces un id válido devuelve el arma correspondiente")
        void unIdValidoDevuelveElArma() {
            assertAll("Ids válidos",
                    () -> assertEquals(ShopWeapon.NONE, ShopWeapon.fromId(0), "El id 0 es NONE"),
                    () -> assertEquals(ShopWeapon.PLASMA_ORB, ShopWeapon.fromId(3), "El id 3 es PLASMA_ORB")
            );
        }

        @ParameterizedTest(name = "Entonces el id {0} devuelve NONE")
        @ValueSource(ints = {-1, -50, 6, 99})
        void unIdFueraDeRangoDevuelveNone(int id) {
            assertEquals(ShopWeapon.NONE, ShopWeapon.fromId(id),
                    "Un id fuera de rango no debería romper el juego");
        }
    }

    @Nested
    @DisplayName("Cuando se comprueba si un arma está desbloqueada")
    class Desbloqueo {

        @Test
        @DisplayName("Entonces NONE y NEEDLE_LASER están disponibles desde el principio")
        void lasArmasBasicasEstanSiempreDesbloqueadas() {
            assertAll("Armas iniciales",
                    () -> assertTrue(ShopWeapon.NONE.isUnlocked(0, 1, 0, 0, 0, 0), "NONE siempre disponible"),
                    () -> assertTrue(ShopWeapon.NEEDLE_LASER.isUnlocked(0, 1, 0, 0, 0, 0),
                            "NEEDLE_LASER siempre disponible")
            );
        }

        @Test
        @DisplayName("Entonces TWIN_BEAM se desbloquea con rango 3 o con la oleada 8")
        void twinBeamSeDesbloqueaConRangoUOleada() {
            assertAll("Condiciones de TWIN_BEAM",
                    () -> assertTrue(ShopWeapon.TWIN_BEAM.isUnlocked(3, 1, 0, 0, 0, 0), "Rango 3 basta"),
                    () -> assertTrue(ShopWeapon.TWIN_BEAM.isUnlocked(0, 8, 0, 0, 0, 0), "Oleada 8 basta"),
                    () -> assertFalse(ShopWeapon.TWIN_BEAM.isUnlocked(2, 7, 0, 0, 0, 0),
                            "Con rango 2 y oleada 7 no debería estar desbloqueada")
            );
        }

        @Test
        @DisplayName("Entonces PLASMA_ORB necesita rango 6 y ganar los minijuegos de meteoritos y cognitivo")
        void plasmaOrbNecesitaRangoYMinijuegos() {
            assertAll("Condiciones de PLASMA_ORB",
                    () -> assertTrue(ShopWeapon.PLASMA_ORB.isUnlocked(6, 1, 1, 1, 0, 0), "Cumple todo"),
                    () -> assertFalse(ShopWeapon.PLASMA_ORB.isUnlocked(5, 1, 1, 1, 0, 0), "Falta rango"),
                    () -> assertFalse(ShopWeapon.PLASMA_ORB.isUnlocked(6, 1, 0, 1, 0, 0), "Falta meteoritos"),
                    () -> assertFalse(ShopWeapon.PLASMA_ORB.isUnlocked(6, 1, 1, 0, 0, 0), "Falta cognitivo")
            );
        }

        @Test
        @DisplayName("Entonces ARC_LANCE necesita rango 10 y 15 combos")
        void arcLanceNecesitaRangoYCombos() {
            assertAll("Condiciones de ARC_LANCE",
                    () -> assertTrue(ShopWeapon.ARC_LANCE.isUnlocked(10, 1, 0, 0, 15, 0), "Cumple todo"),
                    () -> assertFalse(ShopWeapon.ARC_LANCE.isUnlocked(10, 1, 0, 0, 14, 0), "Faltan combos"),
                    () -> assertFalse(ShopWeapon.ARC_LANCE.isUnlocked(9, 1, 0, 0, 15, 0), "Falta rango")
            );
        }

        @Test
        @DisplayName("Entonces SWEEP_LASER necesita rango 15, un jefe derrotado y la oleada 20")
        void sweepLaserNecesitaRangoJefeYOleada() {
            assertAll("Condiciones de SWEEP_LASER",
                    () -> assertTrue(ShopWeapon.SWEEP_LASER.isUnlocked(15, 20, 0, 0, 0, 1), "Cumple todo"),
                    () -> assertFalse(ShopWeapon.SWEEP_LASER.isUnlocked(15, 19, 0, 0, 0, 1), "Falta oleada"),
                    () -> assertFalse(ShopWeapon.SWEEP_LASER.isUnlocked(15, 20, 0, 0, 0, 0), "Falta jefe"),
                    () -> assertFalse(ShopWeapon.SWEEP_LASER.isUnlocked(14, 20, 0, 0, 0, 1), "Falta rango")
            );
        }
    }

    @Nested
    @DisplayName("Cuando se muestra la pista de desbloqueo")
    class PistaDeDesbloqueo {

        @Test
        @DisplayName("Entonces las armas iniciales no tienen pista")
        void lasArmasInicialesNoTienenPista() {
            assertAll("Armas sin pista",
                    () -> assertEquals("", ShopWeapon.NONE.unlockHint(), "NONE no necesita pista"),
                    () -> assertEquals("", ShopWeapon.NEEDLE_LASER.unlockHint(), "NEEDLE_LASER no necesita pista")
            );
        }

        @ParameterizedTest(name = "Entonces {0} explica cómo desbloquearse")
        @EnumSource(value = ShopWeapon.class, names = {"TWIN_BEAM", "PLASMA_ORB", "ARC_LANCE", "SWEEP_LASER"})
        void lasArmasBloqueadasTienenPista(ShopWeapon arma) {
            assertFalse(arma.unlockHint().isBlank(), arma + " debería tener una pista de desbloqueo");
        }
    }

    @Nested
    @DisplayName("Dado el catálogo de armas de la tienda")
    class CatalogoDeArmas {

        @Test
        @DisplayName("Entonces no incluye NONE y está ordenado del más caro al más barato")
        void elCatalogoNoIncluyeNoneYEstaOrdenado() {
            ShopWeapon[] armas = ShopWeapon.shopItems();
            assertFalse(Arrays.asList(armas).contains(ShopWeapon.NONE), "NONE no se vende en la tienda");
            for (int i = 1; i < armas.length; i++) {
                assertTrue(armas[i - 1].price() >= armas[i].price(),
                        armas[i - 1] + " debería costar más que " + armas[i]);
            }
        }

        @ParameterizedTest(name = "Entonces {0} tiene nombre, tipo, descripción y color")
        @EnumSource(value = ShopWeapon.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
        void cadaArmaTieneSusDatos(ShopWeapon arma) {
            assertAll("Datos de " + arma,
                    () -> assertTrue(arma.price() > 0, "Precio positivo"),
                    () -> assertFalse(arma.displayName().isBlank(), "Nombre visible"),
                    () -> assertFalse(arma.displayName().contains("."), "El nombre está traducido"),
                    () -> assertFalse(arma.typeTag().isBlank(), "Etiqueta de tipo"),
                    () -> assertFalse(arma.description().isBlank(), "Descripción"),
                    () -> assertNotNull(arma.colorTag(), "Color")
            );
        }
    }
}