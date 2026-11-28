# Análisis estático y de seguridad

Evaluación de los resultados de PMD, SpotBugs y OWASP Dependency-Check sobre DeltaBlade (issue #10).

## Cómo se ejecuta

Las tres herramientas están configuradas en el `pom.xml` con versiones fijadas y se ejecutan en la fase `verify`:

```bash
mvn verify -DnvdApiKey=TU_CLAVE_NVD
```

| Herramienta | Versión | Qué analiza | Informe |
|---|---|---|---|
| Maven PMD Plugin | 3.27.0 (PMD 7.14.0) | Código fuente (`src/main/java`) | `target/pmd.xml`, `target/site/pmd.html` |
| SpotBugs Maven Plugin | 4.9.8.5 (SpotBugs 4.9.8) | Bytecode (`target/classes`) | `target/spotbugsXml.xml`, `target/spotbugs.html` |
| OWASP Dependency-Check | 12.1.9 | Dependencias (directas y transitivas) | `target/dependency-check-report.html` |

La clave de la API del NVD se pasa por línea de comandos y nunca se sube al repositorio. En el workflow de release se omite Dependency-Check con `-DdependencyCheck.skip=true`, porque allí no hay clave.

### Cuándo falla el build

| Herramienta | Condición de fallo |
|---|---|
| PMD | Violaciones de prioridad 1 o 2 (`failurePriority = 2`) |
| SpotBugs | Cualquier aviso de prioridad media o alta que no esté en `spotbugs-exclude.xml` |
| Dependency-Check | Alguna vulnerabilidad con CVSS ≥ 7 que no esté en `dependency-check-suppressions.xml` |

## 1. PMD

**Resultado inicial:** 43 violaciones (1 de prioridad 2, 19 de prioridad 3 y 23 de prioridad 4) con el conjunto de reglas por defecto del plugin.

### Corregidas

| Regla | Dónde | Corrección |
|---|---|---|
| UnusedPrivateField | `DeltaBladeApp.laserTickTimer`, `DeltaBladeFactory.BOSS_SIZE`, `OptionsOverlay.VolumeBar.height`, `EnemyComponent.speedX` y `hoverAmplitude`, `ExtraLetterPickupComponent.letterText` | Campos eliminados junto con sus asignaciones |
| UnusedLocalVariable | `DeltaBladeApp.applyPlasmaHit` (`ec`), `MazeEscapeGame.buildSettlementOverlay` (`panelH`) | Variables eliminadas |
| UnusedPrivateMethod | `DeltaBladeApp.hasScoopEscorts()` | Método eliminado (código muerto) |
| UnusedFormalParameter | `EmbeddedTextures.decodePng(byte[], String)` | Parámetro `name` eliminado |

### Justificadas (no se corrigen)

| Regla | Prioridad | Motivo |
|---|---|---|
| AvoidBranchingStatementAsLastInLoop (`MusicLocations.userDirsMusicDir`) | 2 | El `return` dentro del bucle es intencionado: devuelve la primera línea `XDG_MUSIC_DIR` válida. Se suprime con `@SuppressWarnings` en ese método |
| CollapsibleIfStatements (5) | 3 | Los `if` anidados separan condiciones con significado distinto y se leen mejor así |
| UselessParentheses (16) | 4 | Paréntesis añadidos a propósito para dejar clara la precedencia en expresiones aritméticas |
| UnnecessaryFullyQualifiedName (7) | 4 | Solo afecta al estilo; no cambia el comportamiento |
| Avisos en `deltablade.tools` (`exportSampleExplosions`, `shockwaveRadius`, `imageWidth`, `imageHeight`) | 3 | Herramientas de desarrollo (editor y generador de explosiones) que no forman parte del juego |

## 2. SpotBugs

**Resultado inicial:** 76 avisos (7 de prioridad alta y 69 de prioridad media), con `effort = Max` y `threshold = Medium`.

### Corregidos

| Patrón | Dónde | Corrección |
|---|---|---|
| NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE (6) | `MusicCatalog`, `MusicLocations` | Se comprueba que `Path.getFileName()` no sea `null` antes de usarlo |
| DM_DEFAULT_ENCODING (alta) | `EmbeddedTextures.decodePng` | `new String(..., StandardCharsets.US_ASCII)` para leer el tipo de chunk PNG |
| OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE | `EmbeddedTextures.getImageNative` | `try-with-resources` para cerrar siempre el `InputStream` |
| MS_MUTABLE_ARRAY (alta, 2) | `RankMarkerComponent.COLORS` y `TEXTURES` | Arrays privados con métodos de acceso `colorCount()`, `colorAt(int)` y `textureAt(int)` |
| MS_PKGPROTECT (2) | `GameVars.EXTRA_LETTERS` y `EXTRA_VARS` | Visibilidad de paquete (solo se usan desde `deltablade`) |
| DMI_RANDOM_USED_ONLY_ONCE (alta) | `AlienEnduranceGame.heartTimer` | Se reutiliza el `Random` del minijuego en lugar de crear uno nuevo |
| ICAST_IDIV_CAST_TO_DOUBLE (2) | `WaveManager.bossEntry()` y entrada del jefe | División entre `2.0`, igual que en el resto de trayectorias |
| URF_UNREAD_FIELD, URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD, DLS_DEAD_LOCAL_STORE | Mismos campos y variables que PMD, y `WaveManager.EntryPath.flightName` | Eliminados |
| EI_EXPOSE_REP / EI_EXPOSE_REP2 (4) | `HighScoreClient.SubmitResult`, `HighScoreOverlay.setGlobalEntries`, `ExtraLetterPickupComponent.letterText` | Copia inmutable de las listas con `List.copyOf`; el campo `letterText` se elimina por no usarse |

### Justificados (excluidos en `spotbugs-exclude.xml`)

| Patrón | Avisos | Motivo |
|---|---|---|
| EI_EXPOSE_REP / EI_EXPOSE_REP2 | 24 | Son nodos de JavaFX (`Group`, `ImageView`, `Circle`…), imágenes y el `MinigameHost`. Se comparten a propósito porque forman el grafo de escena; copiarlos rompería el juego |
| DMI_RANDOM_USED_ONLY_ONCE (`WaveManager`) | 2 | `new Random(semilla)` con semilla de la partida y la oleada: el tipo de oleada debe ser reproducible dentro de una misma partida |
| IM_BAD_CHECK_FOR_ODD (`WaveManager`) | 5 | Los valores comprobados (nivel, fila, índice de escuadrón) nunca son negativos |
| ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD (`WaveManager`) | 2 | Solo hay un `WaveManager` por partida; la semilla y el reloj de formación son estado global de la partida |
| REC_CATCH_EXCEPTION | 3 | Captura genérica intencionada (firma HMAC, comando `xdg-user-dir`, carga de imágenes): si falla, se usa un valor por defecto y el juego sigue |
| DE_MIGHT_IGNORE | 2 | Si el dispositivo de audio no admite control de volumen, se ignora y el sonido suena con el volumen por defecto |
| RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE | 1 | Comprobación defensiva sobre un valor de FXGL anotado como `@NotNull` |
| DM_NEXTINT_VIA_NEXTDOUBLE | 2 | Mejora de rendimiento despreciable (una llamada por enemigo u oleada) |
| Paquete `deltablade.tools` | 12 | Herramientas de desarrollo; no se ejecutan durante el juego |

## 3. OWASP Dependency-Check

**Resultado inicial:** 3 dependencias con vulnerabilidades conocidas, todas transitivas o de audio.

| Dependencia | CVE | Severidad máx. | Decisión |
|---|---|---|---|
| jackson-databind 2.14.2 (transitiva de FXGL) | CVE-2026-54512, CVE-2026-54513, CVE-2026-54514, CVE-2026-54515, CVE-2023-35116 | Alta (8.1) | **Corregida:** se fuerza Jackson 2.18.9 con el BOM `jackson-bom` en `dependencyManagement`. Es la versión que corrige las cuatro CVE de 2026; CVE-2023-35116 solo afecta hasta la 2.15.x |
| jlayer 1.0.1 | 60 CVE de Zoom (CVE-2018-15715, CVE-2023-36534…) | — | **Falso positivo:** el decodificador MP3 de JavaZOOM se identifica por error con el CPE `zoom:zoom` (la aplicación de videoconferencias). Suprimido |
| kotlin-stdlib 1.8.10 (transitiva de FXGL) | CVE-2026-53914 | Crítica (9.8) | **Falso positivo:** afecta a los metadatos de la caché de compilación de Kotlin, no a la biblioteca estándar en tiempo de ejecución. Suprimido |
| kotlin-stdlib 1.8.10 | CVE-2020-29582 | Media (5.3) | **Falso positivo:** corregida en Kotlin 1.4.21; el proyecto usa la 1.8.10. Suprimido |

Las vulnerabilidades de Jackson afectan a la deserialización polimórfica de JSON no confiable. DeltaBlade no la usa directamente (el JSON de las puntuaciones se procesa a mano en `HighScoreClient`), pero se actualiza igualmente porque el cambio es compatible y elimina el riesgo.

## Resumen

| Herramienta | Encontrados | Corregidos | Justificados |
|---|---|---|---|
| PMD | 43 | 10 | 33 (estilo y herramientas de desarrollo) |
| SpotBugs | 76 | 23 | 53 (excluidos con motivo) |
| Dependency-Check | 3 dependencias | 1 (Jackson actualizado) | 2 (falsos positivos) |

Tras las correcciones, `mvn verify` ejecuta las tres herramientas y solo falla si aparecen problemas nuevos de PMD con prioridad 1-2, avisos de SpotBugs no evaluados o vulnerabilidades con CVSS ≥ 7.
