package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

/**
 * Componente FXGL de las monedas que sueltan los enemigos y que el jugador recoge.
 */
public class CoinComponent extends Component {

    /**
     * Tipos de moneda con su valor y su velocidad de caída.
     */
    public enum CoinType {
        WHITE(10, 80),
        GREEN(50, 80),
        BLUE(100, 80),
        VIOLET(200, 240);

        public final int value;
        public final double fallSpeed;

        CoinType(int value, double fallSpeed) {
            this.value = value;
            this.fallSpeed = fallSpeed;
        }
    }

    private final CoinType coinType;

    /**
     * Crea una moneda del tipo indicado.
     *
     * @param coinType tipo de moneda
     */
    public CoinComponent(CoinType coinType) {
        this.coinType = coinType;
    }

    /**
     * Devuelve el valor de la moneda.
     *
     * @return valor en dólares del juego
     */
    public int getValue() {
        return coinType.value;
    }

    /**
     * Devuelve el tipo de moneda.
     *
     * @return tipo de moneda
     */
    public CoinType getCoinType() {
        return coinType;
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateY(coinType.fallSpeed * tpf);

        if (entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }
}
