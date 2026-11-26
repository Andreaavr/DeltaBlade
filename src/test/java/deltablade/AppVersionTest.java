package deltablade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de {@link AppVersion}.
 */
@DisplayName("Versión de la aplicación")
class AppVersionTest {

    @Test
    @DisplayName("Entonces la versión tiene el formato X.Y.Z")
    void laVersionTieneFormatoSemantico() {
        String version = AppVersion.current();
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"),
                "La versión debería tener el formato X.Y.Z y es: " + version);
    }

    @Test
    @DisplayName("Entonces la etiqueta es la versión precedida de 'v'")
    void laEtiquetaEmpiezaPorV() {
        assertEquals("v" + AppVersion.current(), AppVersion.label(),
                "La etiqueta debería ser 'v' + versión");
    }
}