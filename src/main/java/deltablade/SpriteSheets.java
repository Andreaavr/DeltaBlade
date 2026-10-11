package deltablade;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpriteSheets {

    public static final String SHIPS = "fightersxy.png";
    public static final String MONSTERS = "fightersalienz.png";
    public static final int CELL = 64;
    public static final int SHIP_TILES = 24;
    public static final int MONSTER_TILES = 5;
    /** Second alien sheet: first row = crab, moth, eye, jelly, leaf, brain. */
    public static final String ALIENS2 = "alienz2.png";
    public static final int ALIEN_CRAB = 0, ALIEN_MOTH = 1, ALIEN_EYE = 2, ALIEN_JELLY = 3, ALIEN_LEAF = 4, ALIEN_BRAIN = 5;
    public static final int ALIEN2_TILES = 6;

    private static final Map<String, Image> TILES = new ConcurrentHashMap<>();

    private SpriteSheets() {}

    public static Image tile(String sheet, int index) {
        String key = sheet + "#" + index;
        Image cached = TILES.get(key);
        if (cached != null) {
            return cached;
        }
        Image src = EmbeddedTextures.getImage(sheet, 512, 512);
        if (src == null || src.getPixelReader() == null) {
            return null;
        }
        int col = Math.floorMod(index, 8);
        int row = Math.floorDiv(index, 8);
        PixelReader reader = src.getPixelReader();
        WritableImage tile = new WritableImage(reader, col * CELL, row * CELL, CELL, CELL);
        TILES.put(key, tile);
        return tile;
    }

    public static int shipTileFor(int variant) {
        return shipTileFor(deltablade.components.EnemyComponent.EnemyType.BASIC, variant);
    }

    public static int shipTileFor(deltablade.components.EnemyComponent.EnemyType type, int variant) {
        int[] tiles = switch (type) {
            case FAST -> new int[] {16, 17, 21, 22};
            case TOUGH -> new int[] {18, 19, 20, 23};
            default -> new int[] {16, 17, 18, 19, 20, 21, 22, 23};
        };
        return tiles[Math.floorMod(variant, tiles.length)];
    }

    public static int monsterTileFor(int variant) {
        return Math.floorMod(variant, MONSTER_TILES);
    }

    /** Alien tile, upscaled crisp (nearest neighbour) by an integer factor; show it with setSmooth(true). */
    public static Image alienCrisp(int index, int factor) {
        String key = "crisp#" + index + "#" + factor;
        Image cached = TILES.get(key);
        if (cached != null) {
            return cached;
        }
        Image src = tile(ALIENS2, Math.floorMod(index, ALIEN2_TILES));
        if (src == null) {
            return null;
        }
        Image big = DeltaBladeFactory.upscaleNearest(src, Math.max(1, factor));
        TILES.put(key, big);
        return big;
    }

    /** ImageView of a new-sheet alien at the given draw size, clean upscale. */
    public static javafx.scene.image.ImageView alienView(int index, double size) {
        int factor = Math.max(1, (int) Math.ceil(size / CELL));
        Image img = alienCrisp(index, factor);
        javafx.scene.image.ImageView v = new javafx.scene.image.ImageView(img);
        v.setFitWidth(size);
        v.setFitHeight(size);
        v.setPreserveRatio(true);
        v.setSmooth(true);
        return v;
    }
}
