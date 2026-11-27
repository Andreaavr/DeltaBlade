package deltablade;

import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

/**
 * Trayectorias de entrada al estilo Galaga. Todos los puntos son coordenadas de pantalla (centro de la nave).
 * Las trayectorias se mantienen por encima del 62 % de la altura de la pantalla, para que no choquen con el jugador.
 */
public final class FlightPaths {

    public enum Kind {
        ARC,          // gran arco desde un lateral
        LOOP,         // baja desde arriba, hace un rizo y vuelve a subir
        S_CURVE,      // zigzag en forma de serpiente desde arriba
        CIRCLE,       // entra por un lateral y da una vuelta alrededor del centro
        HOOK,         // baja en vertical y luego hace un gancho de vuelta hacia arriba
        SPIRAL,       // círculo que se va cerrando
        WAVE          // onda suave que cruza el campo
    }

    private FlightPaths() {}

    public static List<Point2D> build(Kind kind, boolean mirror) {
        double w = getAppWidth();
        double h = getAppHeight();
        double left = GameVars.RAIL_WIDTH + 30;
        double right = w - GameVars.RAIL_WIDTH - 30;
        double maxY = h * 0.62;
        List<Point2D> pts = new ArrayList<>();
        switch (kind) {
            case ARC -> bezier(pts, -40, h * 0.12, w * 0.30, -20, w * 0.95, h * 0.30, w * 0.55, maxY * 0.85, 40);
            case LOOP -> {
                line(pts, w * 0.28, -40, w * 0.28, h * 0.38, 12);
                double r = 70;
                circle(pts, w * 0.28 + r, h * 0.38, r, Math.PI, Math.PI + 2 * Math.PI, 36);
            }
            case S_CURVE -> {
                int n = 50;
                for (int i = 0; i <= n; i++) {
                    double t = i / (double) n;
                    pts.add(new Point2D(w / 2.0 + Math.sin(t * Math.PI * 2) * w * 0.30, -40 + t * (maxY * 0.8 + 40)));
                }
            }
            case CIRCLE -> {
                double cx = w / 2.0;
                double cy = h * 0.33;
                double r = Math.min(130, (right - left) / 2.6);
                line(pts, -40, cy - r, cx, cy - r, 10);
                circle(pts, cx, cy, r, -Math.PI / 2, -Math.PI / 2 + 2 * Math.PI, 48);
            }
            case HOOK -> {
                line(pts, left + 20, -40, left + 60, maxY, 20);
                bezier(pts, left + 60, maxY, left + 90, maxY + 40, w * 0.55, maxY + 10, w * 0.62, h * 0.28, 24);
            }
            case SPIRAL -> {
                double cx = w / 2.0;
                double cy = h * 0.32;
                line(pts, w + 40, h * 0.10, cx + 140, cy, 10);
                int n = 60;
                for (int i = 0; i <= n; i++) {
                    double t = i / (double) n;
                    double a = t * Math.PI * 3.0;
                    double r = 140 * (1 - t * 0.65);
                    pts.add(new Point2D(cx + Math.cos(a) * r, cy + Math.sin(a) * r * 0.8));
                }
            }
            case WAVE -> {
                int n = 50;
                for (int i = 0; i <= n; i++) {
                    double t = i / (double) n;
                    double x = -40 + t * (w + 80) * 0.85;
                    pts.add(new Point2D(x, h * 0.30 + Math.sin(t * Math.PI * 3) * h * 0.14));
                }
            }
        }
        List<Point2D> out = new ArrayList<>(pts.size());
        for (Point2D p : pts) {
            double x = mirror ? w - p.getX() : p.getX();
            out.add(new Point2D(x, Math.min(p.getY(), maxY + 40)));
        }
        return out;
    }

    /** Trayectorias de cruce para Alien Endurance: atraviesan toda la pantalla y vuelven a salir. */
    public enum Cross {
        FIGURE_EIGHT, // figura de ocho
        ZIGZAG,       // zigzag hacia abajo
        SWARM,        // enjambre suelto que se tambalea
        RAIN          // lluvia en diagonal muy inclinada
    }

    public static List<Point2D> crossing(Cross kind, boolean mirror, double seed) {
        double w = getAppWidth();
        double h = getAppHeight();
        List<Point2D> pts = new ArrayList<>();
        switch (kind) {
            case FIGURE_EIGHT -> {
                double cx = w / 2.0;
                double cy = h * 0.34;
                line(pts, -40, cy, cx - 120, cy, 8);
                int n = 90;
                for (int i = 0; i <= n; i++) {
                    double t = i / (double) n * Math.PI * 2;
                    pts.add(new Point2D(cx - Math.cos(t) * 120, cy + Math.sin(2 * t) * 70));
                }
                line(pts, cx - 120, cy, w + 60, h * 0.55, 10);
            }
            case ZIGZAG -> {
                double left = GameVars.RAIL_WIDTH + 30;
                double right = w - GameVars.RAIL_WIDTH - 30;
                double y = -40;
                boolean toRight = true;
                pts.add(new Point2D(left, y));
                for (int i = 0; i < 6; i++) {
                    y += h * 0.12;
                    line(pts, toRight ? left : right, y - h * 0.12, toRight ? right : left, y, 10);
                    toRight = !toRight;
                }
                pts.add(new Point2D(w / 2.0, h + 60));
            }
            case SWARM -> {
                double x = w * (0.25 + 0.5 * seed);
                int n = 80;
                for (int i = 0; i <= n; i++) {
                    double t = i / (double) n;
                    pts.add(new Point2D(x + Math.sin(t * 9 + seed * 6) * 70 + Math.sin(t * 23) * 18,
                            -40 + t * (h + 100)));
                }
            }
            case RAIN -> {
                double x0 = w * (0.1 + 0.3 * seed);
                line(pts, x0, -40, x0 + w * 0.5, h + 60, 30);
            }
        }
        if (!mirror) {
            return pts;
        }
        List<Point2D> out = new ArrayList<>(pts.size());
        for (Point2D p : pts) {
            out.add(new Point2D(w - p.getX(), p.getY()));
        }
        return out;
    }

    private static void line(List<Point2D> pts, double x0, double y0, double x1, double y1, int n) {
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            pts.add(new Point2D(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t));
        }
    }

    private static void circle(List<Point2D> pts, double cx, double cy, double r, double a0, double a1, int n) {
        for (int i = 0; i <= n; i++) {
            double a = a0 + (a1 - a0) * i / n;
            pts.add(new Point2D(cx + Math.cos(a) * r, cy + Math.sin(a) * r));
        }
    }

    private static void bezier(List<Point2D> pts, double x0, double y0, double x1, double y1,
                               double x2, double y2, double x3, double y3, int n) {
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double u = 1 - t;
            double x = u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3;
            double y = u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3;
            pts.add(new Point2D(x, y));
        }
    }
}
