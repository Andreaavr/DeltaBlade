package deltablade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de {@link ShopItem}.
 */
@DisplayName("Tienda del juego")
class ShopItemTest {

    @Nested
    @DisplayName("Dado el catálogo de la tienda")
    class Catalogo {

        @Test
        @DisplayName("Entonces está ordenado del más caro al más barato")
        void elCatalogoEstaOrdenadoDelMasCaroAlMasBarato() {
            ShopItem[] catalogo = ShopItem.catalog();
            for (int i = 1; i < catalogo.length; i++) {
                assertTrue(catalogo[i - 1].price() >= catalogo[i].price(),
                        catalogo[i - 1] + " debería costar más que " + catalogo[i]);
            }
        }

        @ParameterizedTest(name = "Entonces {0} tiene un precio positivo")
        @EnumSource(ShopItem.class)
        void todosLosObjetosTienenPrecioPositivo(ShopItem item) {
            assertTrue(item.price() > 0, item + " tiene un precio no válido");
        }
    }

    @Nested
    @DisplayName("Dado un objeto que no es un arma")
    class ObjetosQueNoSonArmas {

        @Test
        @DisplayName("Entonces está desbloqueado desde el principio")
        void losObjetosQueNoSonArmasEstanSiempreDesbloqueados() {
            assertAll("Objetos disponibles al empezar la partida",
                    () -> assertTrue(ShopItem.EXTRA_LIFE.isUnlocked(0, 1, 0, 0, 0, 0),
                            "EXTRA_LIFE debería estar desbloqueado"),
                    () -> assertTrue(ShopItem.DOUBLE.isUnlocked(0, 1, 0, 0, 0, 0),
                            "DOUBLE debería estar desbloqueado"),
                    () -> assertTrue(ShopItem.AUTOFIRE.isUnlocked(0, 1, 0, 0, 0, 0),
                            "AUTOFIRE debería estar desbloqueado")
            );
        }

        @Test
        @DisplayName("Entonces no muestra pista de desbloqueo")
        void losObjetosQueNoSonArmasNoTienenPista() {
            assertEquals("", ShopItem.EXTRA_LIFE.unlockHint(),
                    "Un objeto siempre disponible no necesita pista");
        }
    }

    @Nested
    @DisplayName("Dada una mejora del arma principal")
    class MejorasDeArma {

        @Test
        @DisplayName("Entonces sube el arma al grado correcto")
        void lasMejorasDeArmaIndicanElGradoCorrecto() {
            assertAll("Grado de cada mejora",
                    () -> assertEquals(2, ShopItem.DOUBLE.targetGrade(), "DOUBLE → grado 2"),
                    () -> assertEquals(3, ShopItem.TRIPLE.targetGrade(), "TRIPLE → grado 3"),
                    () -> assertEquals(4, ShopItem.QUAD.targetGrade(), "QUAD → grado 4")
            );
        }
    }
}