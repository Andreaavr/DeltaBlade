package deltablade;

import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

/**
 * Galaga-artige Einflug-Wege. Alle Punkte sind Bildschirm-Koordinaten (Mitte des Schiffs).
 * Die Wege bleiben oberhalb von ca. 62 % der Bildhoehe, damit der Spieler nicht gerammt wird.
 */
public final class FlightPaths {

    public enum Kind {
        BOGEN,       // grosser Bogen von der Seite
        SCHLEIFE,    // von oben runter, eine Schleife, dann hoch
        S_KURVE,     // Schlangenlinie von oben
        KREIS,       // von der Seite rein, ein Kreis um die Mitte
        HAKEN,       // steil runter, dann Haken zurueck nach oben
        SPIRALE,     // enger werdender Kreis
        WELLE        // flache Welle quer ueber das Feld
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
            case BOGEN -> bezier(pts, -40, h * 0.12, w * 0.30, -20, w * 0.95, h * 0.30, w * 0.55, maxY * 0.85, 40);
            case SCHLEIFE -> {
                line(pts, w * 0.28, -40, w * 0.28, h * 0.38, 12);
                double r = 70;
                circle(pts, w * 0.28 + r, h * 0.38, r, Math.PI, Math.PI + 2 * Math.PI, 36);
            }
            case S_KURVE -> {
                int n = 50;
                for (int i = 0; i <= n; i++) {
                    double t = i / (double) n;
                    pts.add(new Point2D(w / 2.0 + Math.sin(t * Math.PI * 2) * w * 0.30, -40 + t * (maxY * 0.8 + 40)));
                }
            }
            case KREIS -> {
                double cx = w / 2.0;
                double cy = h * 0.33;
                double r = Math.min(130, (right - left) / 2.6);
                line(pts, -40, cy - r, cx, cy - r, 10);
                circle(pts, cx, cy, r, -Math.PI / 2, -Math.PI / 2 + 2 * Math.PI, 48);
            }
            case HAKEN -> {
                line(pts, left + 20, -40, left + 60, maxY, 20);
                bezier(pts, left + 60, maxY, left + 90, maxY + 40, w * 0.55, maxY + 10, w * 0.62, h * 0.28, 24);
            }
            case SPIRALE -> {
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
            case WELLE -> {
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
