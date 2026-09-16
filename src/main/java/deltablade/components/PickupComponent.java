package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

public class PickupComponent extends Component {
    
    public enum PickupType {
        WEAPON_UPGRADE,
        EXTRA_AMMO,
        EXTRA_LIFE,
        AUTOFIRE,
        METEOR,
        COGNITIVE,
        EXTRA_TIME,
        SHIELD,
        SCOOP,
        MISSILE,
        SKULL
    }
    
    private PickupType type;
    private double speed = 80;
    private double pulsePhase = 0;
    
    public PickupComponent(PickupType type) {
        this(type, 80);
    }

    public PickupComponent(PickupType type, double speed) {
        this.type = type;
        this.speed = speed;
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        
        pulsePhase += tpf * 8;
        double scale = 1 + Math.sin(pulsePhase) * 0.15;
        entity.setScaleX(scale);
        entity.setScaleY(scale);
        
        if (entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }
    
    public PickupType getType() {
        return type;
    }
}
