package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;

public class FadePuffComponent extends Component {

    private final double maxLife;
    private final double driftX;
    private final double driftY;
    private double life;

    public FadePuffComponent(double life, double driftX, double driftY) {
        this.maxLife = Math.max(0.05, life);
        this.life = this.maxLife;
        this.driftX = driftX;
        this.driftY = driftY;
    }

    @Override
    public void onUpdate(double tpf) {
        life -= tpf;
        if (life <= 0 || entity == null) {
            if (entity != null && entity.isActive()) {
                entity.removeFromWorld();
            }
            return;
        }
        double t = Math.max(0, life / maxLife);
        entity.translate(driftX * tpf, driftY * tpf);
        entity.setScaleX(0.45 + t * 0.7);
        entity.setScaleY(0.45 + t * 0.7);
        entity.setOpacity(t * t);
        if (!entity.getViewComponent().getChildren().isEmpty()) {
            Node view = entity.getViewComponent().getChildren().get(0);
            view.setOpacity(t);
        }
    }
}
