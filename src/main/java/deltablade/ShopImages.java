package deltablade;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Imágenes de la tienda. Primera opción: assets/textures/shop/shop_{@literal <objeto>}.png (128x128, transparente).
 * Si no existe, una imagen del juego ampliada píxel a píxel para que quede nítida. null = "NO IMAGE".
 */
final class ShopImages {

    private static final Map<ShopItem, Image> CACHE = new EnumMap<>(ShopItem.class);
    private static final int TARGET = 128;

    private ShopImages() {}

    static String fileName(ShopItem item) {
        return "shop_" + item.name().toLowerCase(Locale.ROOT) + ".png";
    }

    static Image imageFor(ShopItem item) {
        if (CACHE.containsKey(item)) {
            return CACHE.get(item);
        }
        Image img = load("shop/" + fileName(item));
        if (img == null) {
            String fallback = switch (item) {
                case DOUBLE, TRIPLE, QUAD -> "pickup_weapon.png";
                case EXTRA_AMMO -> "pickup_ammo.png";
                case EXTRA_TIME -> "pickup_time.png";
                case AUTOFIRE -> "pickup_autofire.png";
                case EXTRA_LIFE -> "heart.png";
                default -> null;
            };
            if (fallback != null) {
                img = load(fallback);
            }
        }
        if (img != null && img.getWidth() < TARGET) {
            int factor = (int) Math.ceil(TARGET / img.getWidth());
            img = DeltaBladeFactory.upscaleNearest(img, factor);
        }
        CACHE.put(item, img);
        return img;
    }

    private static Image load(String name) {
        try (InputStream is = ShopImages.class.getResourceAsStream("/assets/textures/" + name)) {
            if (is == null) {
                return null;
            }
            Image img = new Image(is);
            if (!img.isError() && img.getWidth() > 0) {
                return img;
            }
        } catch (Exception ignored) {
        }
        try {
            Image img = EmbeddedTextures.getImageNative(name);
            return img != null && img.getWidth() > 0 ? img : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
