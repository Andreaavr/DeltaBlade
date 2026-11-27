# DeltaBlade

**Matamarcianos arcade original** inspirado en clásicos como Galaga. Desarrollado con Java 21 y el motor de juegos [FXGL](https://github.com/AlmasB/FXGL).

![Java](https://img.shields.io/badge/Java-21-orange)
![FXGL](https://img.shields.io/badge/FXGL-21.1-blue)
![License](https://img.shields.io/badge/License-MIT-green)

> 🇪🇸 Español · 🇬🇧 [English version below](#-english)

---

## Índice
- [Versión mínima de Java](#-versión-mínima-de-java)
- [Requisitos](#requisitos)
- [Cómo ejecutarlo](#cómo-ejecutarlo)
- [JAR ejecutable](#jar-ejecutable)
- [Instaladores (Mac, Windows, Linux)](#instaladores-mac-windows-linux)
- [Idioma del juego](#idioma-del-juego)
- [Cómo se juega](#cómo-se-juega)
- [Mecánicas del juego](#mecánicas-del-juego)
- [Pruebas y calidad](#pruebas-y-calidad)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Editor de explosiones](#-editor-de-explosiones)
- [Compilación](#compilación)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Créditos](#créditos)

---

## ☕ Versión mínima de Java

**DeltaBlade necesita Java 21 o superior** para compilar y ejecutarse.

### Motivo
El código usa los métodos `getFirst()` y `getLast()` de `java.util.List`, que se añadieron en **Java 21** con las colecciones secuenciadas ([JEP 431](https://openjdk.org/jeps/431)):

| Fichero | Uso |
|---|---|
| `HighScoreStore.java` | `table.getLast()` |
| `DeltaBladeApp.java` | `table.getFirst()` |
| `MusicCatalog.java` | `tracks.getFirst()` (2 veces) |

### Cómo se ha comprobado
Compilando el proyecto con versiones anteriores de Java:

| Comando | Resultado |
|---|---|
| `mvn clean compile -Dmaven.compiler.release=17` | ❌ 4 errores: `cannot find symbol: method getFirst()/getLast()` |
| `mvn clean compile -Dmaven.compiler.release=20` | ❌ los mismos 4 errores |
| `mvn clean compile -Dmaven.compiler.release=21` | ✅ BUILD SUCCESS |

### Comprobación automática
El `pom.xml` incluye `maven-enforcer-plugin`, que en la fase `validate` comprueba que se usa **Java 21+** y **Maven 3.6.3+**. Con una versión inferior, la compilación se detiene con este mensaje:

```
DeltaBlade necesita Java 21 o superior (usa List.getFirst()/getLast(), añadidos en Java 21).
```

Para comprobarlo:
```bash
mvn validate
```

---

## Requisitos
- **JDK 21** o superior
- **Maven 3.6.3** o superior

## Cómo ejecutarlo
```bash
mvn javafx:run
```

En macOS, el juego desactiva automáticamente el renderizado Metal por compatibilidad. Si aun así hay problemas de visualización, prueba:
```bash
mvn javafx:run -Djavafx.options="-Dprism.order=sw"
```

O como alternativa:
```bash
mvn compile exec:java
```

## JAR ejecutable
`mvn package` genera un JAR que incluye todas las dependencias (con `maven-shade-plugin`), así que se puede ejecutar sin IDE desde cualquier carpeta:
```bash
mvn package
java -jar target/deltablade-1.0.8.jar
```
El JAR incluye las librerías nativas de JavaFX del sistema operativo en el que se compila, por lo que funciona en ese mismo sistema.

## Instaladores (Mac, Windows, Linux)
La versión se toma del `pom.xml` (`1.0.8`). Los paquetes se llaman `DeltaBlade-1.0.8-<so>-<arquitectura>`.

Compilación local para el sistema actual (necesita JDK 21 con `jpackage`):
```bash
mvn -Pdist verify
```
Resultado: `target/dist/`

- macOS: `.dmg` (sin firmar: después de descargarlo, usa `Installieren.command` dentro del DMG, o `xattr -cr` sobre el DMG/la app si Gatekeeper dice que está "dañado")
- Windows: `.exe` (necesita [WiX 3](https://wixtoolset.org/))
- Linux: `.deb` (necesita `fakeroot`), tarball para Arch y `.AppImage`

Las tres plataformas se compilan con GitHub Actions (workflow **Package**, o con una etiqueta `v1.0.8`).

## Idioma del juego
El juego está disponible en **español** (por defecto) e **inglés**. El idioma se cambia en **OPCIONES → IDIOMA** y se aplica al reiniciar el juego.

Los textos están en `src/main/resources/i18n/`:
- `messages_es.properties`: español
- `messages_en.properties`: inglés

## Cómo se juega
- **Mover**: flechas (← →) o A/D
- **Disparar**: Espacio o X
- **Reiniciar**: R (después de perder)
- **Opciones**: ESC

## Mecánicas del juego

### Sistema de oleadas
- **Entrada en escuadrón**: los enemigos entran en grupos siguiendo trayectorias curvas y después se colocan en formación.
- **Bonus de combo de escuadrón**: si destruyes un escuadrón entero antes de que se coloque en formación, ganas puntos extra.
- **Ataques en picado**: una vez en formación, los enemigos pueden lanzarse en picado hacia el jugador.

### Combate
- **Munición limitada**: empiezas con 5 disparos simultáneos. Se libera un hueco cuando una bala impacta o sale de la pantalla.
- **Explosiones**: animaciones de explosión en pixel-art al impactar las balas y al destruir enemigos o al jugador.
- **Mejoras de arma**: recoge los objetos dorados (W) para mejorar el arma:
  - Grado 1: disparo simple
  - Grado 2: disparo doble
  - Grado 3: disparo triple
- **Munición extra**: los objetos cian (+) aumentan la capacidad (máximo 12).
- **Penalización**: al recibir un impacto, el arma baja un grado (mínimo grado 1).

### Enemigos
- **Básico (rojo)**: enemigo normal, 1 impacto.
- **Rápido (verde)**: ágil y veloz, 1 impacto.
- **Resistente (morado)**: blindado, necesita 3 impactos.

### Supervivencia
- **Vidas**: 3 vidas en total.
- **Invulnerabilidad**: unos instantes de protección después de recibir un impacto.

## Pruebas y calidad
```bash
mvn test      # pruebas unitarias con JUnit 5
mvn verify    # pruebas + cobertura con JaCoCo (mínimo 70 % de líneas) + Checkstyle
```

---

## Estructura del proyecto

```
DeltaBlade/
├── pom.xml                              # Configuración de Maven (FXGL 21.1)
├── checkstyle.xml                       # Convención de estilo del código
├── src/main/java/deltablade/
│   ├── DeltaBladeApp.java               # Aplicación principal de FXGL
│   ├── Launcher.java                    # Punto de entrada del JAR ejecutable
│   ├── I18n.java                        # Traducción de los textos (español/inglés)
│   ├── DeltaBladeFactory.java           # Fábrica de entidades (aparición de objetos)
│   ├── WaveManager.java                 # Aparición de escuadrones y sistema de combos
│   ├── EntityType.java                  # Enumerado de tipos de entidad
│   ├── GameVars.java                    # Variables y constantes del juego
│   ├── EmbeddedTextures.java            # Decodificador de PNG (solo RGBA de 8 bits)
│   ├── components/
│   │   ├── PlayerComponent.java         # Movimiento del jugador, disparo e invulnerabilidad
│   │   ├── EnemyComponent.java          # IA enemiga: entrada, formación y picado
│   │   ├── BulletComponent.java         # Movimiento de los proyectiles
│   │   ├── PickupComponent.java         # Comportamiento de los objetos recogibles
│   │   └── ExplosionComponent.java      # Reproducción de animaciones de sprites
│   └── tools/
│       └── ExplosionEditor.java         # Editor independiente de explosiones
├── src/main/resources/                  # Recursos (texturas, música, textos i18n)
└── src/test/java/deltablade/            # Pruebas unitarias (JUnit 5)
```

---

## 🎆 Editor de explosiones

Herramienta independiente para diseñar explosiones procedurales en pixel-art y exportarlas como hojas de sprites animadas.

Las explosiones ya están integradas en el juego mediante `ExplosionComponent`. Este editor sirve para crear efectos de explosión personalizados.

### Cómo abrir el editor
```bash
mvn javafx:run -Pexplosions
```

### Funciones
- **Vista previa en directo a 60 fps** con renderizado *nearest-neighbor* (estilo pixel-art).
- **Parámetros**: duración, número de fotogramas, tamaño de fotograma (32/48/64/96), número de partículas, tamaño a lo largo de la vida, degradado de color (núcleo/medio/humo), gravedad, velocidad hacia fuera y rozamiento.
- **Capas activables**: fuego, chispas, humo y onda expansiva.
- **Ajustes predefinidos**: impacto (chispas pequeñas), nave (fuego medio y restos), resistente (naranja más grande), jefe (grande y con varias capas), plasma (cian/magenta) y chispas.
- **Reproducción**: reproducir, pausar, reiniciar, avanzar fotograma a fotograma y bucle.
- **Semilla**: explosiones reproducibles mediante el campo de semilla y el botón de aleatorio.

### Exportar
1. **Hoja de sprites**: pulsa "Exportar hoja de sprites..." → guarda un PNG y un JSON con los metadatos.
2. **Fotogramas sueltos**: pulsa "Exportar fotogramas..." → guarda PNG numerados y un JSON.

Los PNG exportados son **RGBA de 8 bits, sin entrelazado** (tipo de color 6), compatibles con `EmbeddedTextures.decodePng()` del juego.

### Integración en el juego
Las explosiones se reproducen con `ExplosionComponent`. Para añadir hojas personalizadas, inclúyelas en `EmbeddedTextures` con claves como `explosion_custom.png` y créalas con:
```java
spawn("explosion", new SpawnData(x, y).put("variant", "custom"));
```

---

## Compilación

```bash
# Solo compilar
mvn compile

# Compilar sin mensajes (no muestra nada si va bien)
mvn -q compile

# Empaquetar como JAR
mvn package

# Compilación limpia e instalación
mvn clean install
```

## Arquitectura

Construido con el sistema Entidad-Componente de FXGL:

- **GameApplication**: clase principal del juego, que extiende `GameApplication` de FXGL.
- **WaveManager**: controla la aparición de escuadrones, sus trayectorias de entrada y los bonus de combo.
- **EntityFactory**: crea las entidades del juego (jugador, enemigos, balas y objetos).
- **Componentes**: comportamiento modular asociado a las entidades.
  - `PlayerComponent`: movimiento, disparo e invulnerabilidad.
  - `EnemyComponent`: máquina de estados (ENTERING → FORMATION → DIVING).
  - `BulletComponent`: física de los proyectiles.
  - `PickupComponent`: animación y efectos de los objetos recogibles.

### Estados de los enemigos
1. **ENTERING**: vuela por una trayectoria curva hasta su hueco en la formación.
2. **FORMATION**: se mantiene en su posición y puede lanzarse en picado al azar.
3. **DIVING**: ataca hacia la posición del jugador.

### Funcionalidades añadidas
Sobre esta arquitectura se han incorporado:
- Tienda entre oleadas
- Escudos
- Jefes finales
- Mecánica de recogida de bonus (*scoop*)
- Efectos de sonido y música
- Minijuegos
- Selector de idioma (español/inglés)

---

## Tecnologías

- **[FXGL](https://github.com/AlmasB/FXGL)**: librería de juegos para JavaFX, de Almas Baimagambetov
- **JavaFX 21**: gráficos e interfaz
- **Java 21**: lenguaje

---

## Créditos

### Recursos de pixel-art
Los sprites de pixel-art del juego proceden del **Space Shooter Pack** de **RGS_Dev (Raphael Gonçalves)**.

- **Paquete**: [Space Shooter Pack - Pixel Art 2D by RGS_Dev](https://rgsdev.itch.io/space-shooter-pack-pixel-art-2d-by-rgsdev)
- **Licencia**: uso libre en juegos. No se permite revenderlos ni redistribuirlos como paquete de recursos.

---

*DeltaBlade es un juego original desarrollado con FXGL y recursos de pixel-art de RGS_Dev.*

---
---

# 🇬🇧 English

**Original arcade shooter game** inspired by classic space shooters like Galaga. Built with Java 21 and the [FXGL](https://github.com/AlmasB/FXGL) game engine.

---

## Table of Contents
- [Minimum Java Version](#-minimum-java-version)
- [Requirements](#requirements)
- [How to Run](#how-to-run)
- [Executable JAR](#executable-jar)
- [Installers (Mac, Windows, Linux)](#installers-mac-windows-linux)
- [Game Language](#game-language)
- [How to Play](#how-to-play)
- [Game Features](#game-features)
- [Tests and Quality](#tests-and-quality)
- [Project Structure](#project-structure)
- [Explosion Editor](#-explosion-editor)
- [Building](#building)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Credits](#credits)

---

## ☕ Minimum Java Version

**DeltaBlade requires Java 21 or higher** to compile and run.

### Reason
The code uses the `getFirst()` and `getLast()` methods of `java.util.List`, which were added in **Java 21** with Sequenced Collections ([JEP 431](https://openjdk.org/jeps/431)):

| File | Usage |
|---|---|
| `HighScoreStore.java` | `table.getLast()` |
| `DeltaBladeApp.java` | `table.getFirst()` |
| `MusicCatalog.java` | `tracks.getFirst()` (twice) |

### How it was verified
By compiling the project with earlier Java versions:

| Command | Result |
|---|---|
| `mvn clean compile -Dmaven.compiler.release=17` | ❌ 4 errors: `cannot find symbol: method getFirst()/getLast()` |
| `mvn clean compile -Dmaven.compiler.release=20` | ❌ the same 4 errors |
| `mvn clean compile -Dmaven.compiler.release=21` | ✅ BUILD SUCCESS |

### Automatic check
The `pom.xml` includes `maven-enforcer-plugin`, which checks in the `validate` phase that **Java 21+** and **Maven 3.6.3+** are used. With an earlier version, the build stops with this message:

```
DeltaBlade necesita Java 21 o superior (usa List.getFirst()/getLast(), añadidos en Java 21).
```

To check it:
```bash
mvn validate
```

---

## Requirements
- **JDK 21** or higher
- **Maven 3.6.3** or higher

## How to Run
```bash
mvn javafx:run
```

The game automatically disables Metal rendering on macOS for compatibility. If you still experience rendering issues, you can try:
```bash
mvn javafx:run -Djavafx.options="-Dprism.order=sw"
```

Or alternatively:
```bash
mvn compile exec:java
```

## Executable JAR
`mvn package` builds a JAR that includes all dependencies (with `maven-shade-plugin`), so it can be run without an IDE from any folder:
```bash
mvn package
java -jar target/deltablade-1.0.8.jar
```
The JAR includes the JavaFX native libraries of the operating system it is built on, so it runs on that same system.

## Installers (Mac, Windows, Linux)
The version comes from `pom.xml` (`1.0.8`). Packages are named `DeltaBlade-1.0.8-<os>-<arch>`.

Local build for the current OS (needs JDK 21 with `jpackage`):
```bash
mvn -Pdist verify
```
Output: `target/dist/`

- macOS: `.dmg` (unsigned: after download use `Installieren.command` in the DMG, or `xattr -cr` on the DMG/app if Gatekeeper says it is "damaged")
- Windows: `.exe` (needs [WiX 3](https://wixtoolset.org/))
- Linux: `.deb` (needs `fakeroot`), Arch tarball and `.AppImage`

All three platforms are built by GitHub Actions (workflow **Package**, or a `v1.0.8` tag).

## Game Language
The game is available in **Spanish** (default) and **English**. The language is changed in **OPTIONS → LANGUAGE** and applies after restarting the game.

The texts are in `src/main/resources/i18n/`:
- `messages_es.properties`: Spanish
- `messages_en.properties`: English

## How to Play
- **Move**: Arrow keys (← →) or A/D
- **Fire**: Space or X
- **Restart**: R (after Game Over)
- **Options**: ESC

## Game Features

### Wave System
- **Squad Entry**: enemies fly in as squads along curved paths and then settle into formation.
- **Squad Combo Bonus**: destroy an entire incoming squad before it settles into formation to earn bonus points.
- **Diving Attacks**: once in formation, enemies may dive toward the player.

### Combat
- **Limited Ammo**: you start with 5 simultaneous bullets. A slot frees up when a bullet hits or leaves the screen.
- **Explosions**: pixel-art explosion animations play on bullet hits and when enemies or the player are destroyed.
- **Weapon Upgrades**: collect golden pickups (W) to upgrade your weapon:
  - Grade 1: single shot
  - Grade 2: double shot
  - Grade 3: triple shot
- **Extra Ammo**: cyan pickups (+) increase capacity (maximum 12).
- **Death Penalty**: getting hit drops the weapon by one grade (minimum grade 1).

### Enemies
- **Basic (red)**: standard enemy, 1 hit.
- **Fast (green)**: quick and agile, 1 hit.
- **Tough (purple)**: armored, takes 3 hits.

### Survival
- **Lives**: 3 lives in total.
- **Invulnerability**: a brief moment of protection after getting hit.

## Tests and Quality
```bash
mvn test      # unit tests with JUnit 5
mvn verify    # tests + JaCoCo coverage (minimum 70 % of lines) + Checkstyle
```

---

## Project Structure

```
DeltaBlade/
├── pom.xml                              # Maven build configuration (FXGL 21.1)
├── checkstyle.xml                       # Code style convention
├── src/main/java/deltablade/
│   ├── DeltaBladeApp.java               # Main FXGL application
│   ├── Launcher.java                    # Entry point of the executable JAR
│   ├── I18n.java                        # Text translation (Spanish/English)
│   ├── DeltaBladeFactory.java           # Entity factory (spawning)
│   ├── WaveManager.java                 # Squad spawning and combo system
│   ├── EntityType.java                  # Entity type enum
│   ├── GameVars.java                    # Game variables and constants
│   ├── EmbeddedTextures.java            # PNG decoder (8-bit RGBA only)
│   ├── components/
│   │   ├── PlayerComponent.java         # Player movement, firing and invulnerability
│   │   ├── EnemyComponent.java          # Enemy AI: entry, formation and diving
│   │   ├── BulletComponent.java         # Projectile movement
│   │   ├── PickupComponent.java         # Collectible behavior
│   │   └── ExplosionComponent.java      # Animated sprite strip playback
│   └── tools/
│       └── ExplosionEditor.java         # Standalone explosion editor
├── src/main/resources/                  # Resources (textures, music, i18n texts)
└── src/test/java/deltablade/            # Unit tests (JUnit 5)
```

---

## 🎆 Explosion Editor

A standalone tool for authoring procedural pixel-art explosions and exporting them as animated sprite sheets.

Explosions are already integrated in the game through `ExplosionComponent`. This editor is used to create custom explosion effects.

### How to open the editor
```bash
mvn javafx:run -Pexplosions
```

### Features
- **Live 60 fps preview** with nearest-neighbor rendering (pixel-art style).
- **Parameters**: duration, frame count, frame size (32/48/64/96), particle count, size over lifetime, color gradient (core/mid/smoke), gravity, outward velocity and drag.
- **Layer toggles**: fire, sparks, smoke and shockwave.
- **Presets**: hit (tiny sparks), ship (medium fire and debris), tough (fatter orange), boss (big and multi-layer), plasma (cyan/magenta) and sparks.
- **Playback**: play, pause, restart, frame scrubbing and loop.
- **Seed**: reproducible explosions through the seed field and the randomize button.

### Exporting
1. **Sprite sheet**: click "Exportar hoja de sprites..." → saves a PNG and a JSON with the metadata.
2. **Individual frames**: click "Exportar fotogramas..." → saves numbered PNGs and a JSON.

Exported PNGs are **8-bit RGBA, non-interlaced** (color type 6), compatible with the game's `EmbeddedTextures.decodePng()`.

### In-Game Integration
Explosions are played through `ExplosionComponent`. To add custom sheets, embed them in `EmbeddedTextures` with keys like `explosion_custom.png` and spawn them with:
```java
spawn("explosion", new SpawnData(x, y).put("variant", "custom"));
```

---

## Building

```bash
# Compile only
mvn compile

# Quiet compile (no output on success)
mvn -q compile

# Package as JAR
mvn package

# Clean build and install
mvn clean install
```

## Architecture

Built with FXGL's Entity-Component system:

- **GameApplication**: main game class, extending FXGL's `GameApplication`.
- **WaveManager**: controls squad spawning, entry paths and combo bonuses.
- **EntityFactory**: creates game entities (player, enemies, bullets and pickups).
- **Components**: modular behavior attached to entities.
  - `PlayerComponent`: movement, firing and invulnerability.
  - `EnemyComponent`: state machine (ENTERING → FORMATION → DIVING).
  - `BulletComponent`: projectile physics.
  - `PickupComponent`: collectible animation and effects.

### Enemy States
1. **ENTERING**: flies along a curved path toward its slot in the formation.
2. **FORMATION**: holds its position and may dive at random.
3. **DIVING**: attacks toward the player's position.

### Added Features
On top of this architecture, the following have been added:
- Shop between waves
- Shields
- Boss enemies
- Bonus collection mechanic (*scoop*)
- Sound effects and music
- Minigames
- Language selector (Spanish/English)

---

## Technologies

- **[FXGL](https://github.com/AlmasB/FXGL)**: JavaFX game library by Almas Baimagambetov
- **JavaFX 21**: graphics and UI
- **Java 21**: language

---

## Credits

### Pixel Art Assets
The pixel art sprites used in this game are from the **Space Shooter Pack** by **RGS_Dev (Raphael Gonçalves)**.

- **Asset Pack**: [Space Shooter Pack - Pixel Art 2D by RGS_Dev](https://rgsdev.itch.io/space-shooter-pack-pixel-art-2d-by-rgsdev)
- **License**: free to use in games. Do not resell or redistribute as an asset pack.

---

*DeltaBlade is an original game built with FXGL and pixel art assets by RGS_Dev.*