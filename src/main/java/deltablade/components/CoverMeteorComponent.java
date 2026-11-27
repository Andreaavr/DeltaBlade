package deltablade.components;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

public class CoverMeteorComponent extends Component {

    private final double spin;
    private final Image image;
    private final double displayW;
    private final double displayH;
    private int health = GameVars.METEOR_HITS;
    private double hitFlash;

    public CoverMeteorComponent(double spin, Image image, double displayW, double displayH) {
        this.spin = spin;
        this.image = image;
        this.displayW = Math.max(1, displayW);
        this.displayH = Math.max(1, displayH);
    }

    @Override
    public void onUpdate(double tpf) {
        entity.rotateBy(spin * tpf);
        if (hitFlash > 0) {
            hitFlash = Math.max(0, hitFlash - tpf);
            applyFlash(hitFlash > 0 ? 0.55 : 0);
        }
    }

    public boolean hit() {
        health--;
        hitFlash = 0.14;
        applyFlash(0.55);
        return health <= 0;
    }

    public int getHealth() {
        return health;
    }

    /** True si la parte visible de esta roca está sobre ese punto del mundo (sprite girado). */
    public boolean coversPoint(double worldX, double worldY) {
        if (entity == null) {
            return false;
        }
        double cx = entity.getX() + displayW / 2.0;
        double cy = entity.getY() + displayH / 2.0;
        double dx = worldX - cx;
        double dy = worldY - cy;
        double rad = Math.toRadians(entity.getRotation());
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double localX = dx * cos + dy * sin;
        double localY = -dx * sin + dy * cos;
        double px = localX + displayW / 2.0;
        double py = localY + displayH / 2.0;
        if (px < 0 || py < 0 || px >= displayW || py >= displayH) {
            return false;
        }
        PixelReader reader = image != null ? image.getPixelReader() : null;
        if (reader == null) {
            double nx = localX / (displayW * 0.46);
            double ny = localY / (displayH * 0.46);
            return nx * nx + ny * ny <= 1;
        }
        int ix = (int) (px / displayW * image.getWidth());
        int iy = (int) (py / displayH * image.getHeight());
        ix = Math.max(0, Math.min((int) image.getWidth() - 1, ix));
        iy = Math.max(0, Math.min((int) image.getHeight() - 1, iy));
        return reader.getColor(ix, iy).getOpacity() >= 0.35;
    }

    public boolean coversShot(Entity shot) {
        if (shot == null || !shot.isActive()) {
            return false;
        }
        double x = shot.getX();
        double y = shot.getY();
        double w = shot.getWidth();
        double h = shot.getHeight();
        return coversPoint(x + w / 2.0, y + h / 2.0)
                || coversPoint(x + w / 2.0, y);
    }

    private void applyFlash(double brightness) {
        if (entity.getViewComponent().getChildren().isEmpty()) {
            return;
        }
        Node view = entity.getViewComponent().getChildren().get(0);
        if (brightness <= 0) {
            view.setEffect(null);
            return;
        }
        ColorAdjust flash = new ColorAdjust();
        flash.setBrightness(brightness);
        view.setEffect(flash);
    }
}
