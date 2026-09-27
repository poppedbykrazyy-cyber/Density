package com.densitysmp.orbitalstrike;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Horizontal (x, z) offsets for each strike pattern. */
public final class TntShapes {
    private static final int[] BASE_RADII = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    private static final Font TEXT_FONT = new Font("SansSerif", Font.BOLD, 12);

    private TntShapes() {
    }

    static int ringRadius(int ring) {
        return ring < BASE_RADII.length ? BASE_RADII[ring] : 100 + (ring - BASE_RADII.length + 1) * 10;
    }

    static int ringCount(int ring, int radius) {
        if (ring == 0) return 40;
        if (ring == 1) return 28;
        if (ring <= 4) return 32;
        if (ring <= 6) return 42;
        return Math.min(48, Math.round(radius * 2 - ring * 1.5F));
    }

    public static List<double[]> compressedNuke(int rings) {
        List<double[]> result = new ArrayList<>();
        for (List<double[]> ring : compressedNukeByRing(rings)) {
            result.addAll(ring);
        }
        return result;
    }

    /** Index 0 is the centre point; index i+1 is ring i, evenly spaced. */
    public static List<List<double[]>> compressedNukeByRing(int rings) {
        List<List<double[]>> ringData = new ArrayList<>();
        List<double[]> center = new ArrayList<>();
        center.add(new double[]{0.0, 0.0});
        ringData.add(center);

        for (int ring = 0; ring < rings; ring++) {
            int r = ringRadius(ring);
            int count = ringCount(ring, r);
            List<double[]> pts = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2 * i / count;
                pts.add(new double[]{Math.cos(angle) * r, Math.sin(angle) * r});
            }
            ringData.add(pts);
        }
        return ringData;
    }

    public static List<double[]> cross(int arm, int thickness) {
        List<double[]> pts = new ArrayList<>();
        int half = thickness / 2;
        for (int x = -arm; x <= arm; x++) {
            for (int w = -half; w <= half; w++) {
                pts.add(new double[]{x, w});
            }
        }
        for (int z = -arm; z <= arm; z++) {
            if (z < -half || z > half) {
                for (int w = -half; w <= half; w++) {
                    pts.add(new double[]{w, z});
                }
            }
        }
        return pts;
    }

    public static List<double[]> xShape(int arm) {
        List<double[]> pts = new ArrayList<>();
        double d = 1.0 / Math.sqrt(2.0);
        for (int i = -arm; i <= arm; i++) {
            pts.add(new double[]{i * d, i * d});
            pts.add(new double[]{i * d, -i * d});
        }
        return pts;
    }

    public static List<double[]> ring(int rMin, int rMax) {
        List<double[]> pts = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int total = (int) (Math.PI * (rMax + rMin) * 1.5);
        for (int i = 0; i < total; i++) {
            double a = Math.PI * 2 * i / total;
            double r = rMin + rng.nextDouble() * (rMax - rMin);
            pts.add(new double[]{Math.cos(a) * r, Math.sin(a) * r});
        }
        return pts;
    }

    /** yaw is the Minecraft yaw in radians, so the line runs the way the player faces. */
    public static List<double[]> line(int len, double yaw) {
        List<double[]> pts = new ArrayList<>();
        double dirX = -Math.sin(yaw);
        double dirZ = Math.cos(yaw);
        for (int i = 0; i <= len; i++) {
            pts.add(new double[]{dirX * i, dirZ * i});
        }
        return pts;
    }

    public static List<double[]> polygon(int n, int scale) {
        double[][] verts = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n - Math.PI / 2;
            verts[i] = new double[]{Math.cos(a) * scale, Math.sin(a) * scale};
        }
        List<double[]> pts = new ArrayList<>();
        traceEdges(pts, verts, scale);
        return pts;
    }

    private static void traceEdges(List<double[]> pts, double[][] verts, int scale) {
        int seg = (int) Math.max(scale * 0.8, 10.0);
        for (int i = 0; i < verts.length; i++) {
            double[] p0 = verts[i];
            double[] p1 = verts[(i + 1) % verts.length];
            for (int s = 0; s <= seg; s++) {
                double f = (double) s / seg;
                pts.add(new double[]{p0[0] + (p1[0] - p0[0]) * f, p0[1] + (p1[1] - p0[1]) * f});
            }
        }
    }

    public static List<double[]> hexagon(int scale) {
        return polygon(6, scale);
    }

    public static List<double[]> pentagon(int scale) {
        return polygon(5, scale);
    }

    public static List<double[]> star(int arm) {
        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{0.0, 0.0});
        for (int j = 0; j < 5; j++) {
            double a = Math.PI * 2 * j / 5.0 - Math.PI / 2;
            double ux = Math.cos(a);
            double uz = Math.sin(a);
            for (int i = 1; i <= arm; i++) {
                pts.add(new double[]{ux * i, uz * i});
            }
        }
        return pts;
    }

    public static List<double[]> starOfDavid(int scale) {
        List<double[]> pts = new ArrayList<>();
        traceEdges(pts, triangle(scale, -Math.PI / 2), scale);
        traceEdges(pts, triangle(scale, Math.PI / 6), scale);
        return pts;
    }

    private static double[][] triangle(int scale, double baseAngle) {
        double[][] verts = new double[3][];
        for (int i = 0; i < 3; i++) {
            double a = baseAngle + Math.PI * 2 * i / 3.0;
            verts[i] = new double[]{Math.cos(a) * scale, Math.sin(a) * scale};
        }
        return verts;
    }

    public static List<double[]> heart(int scale) {
        List<double[]> pts = new ArrayList<>();
        double factor = scale / 16.0;
        int seg = Math.max(scale * 8, 60);
        for (int i = 0; i < seg; i++) {
            double t = Math.PI * 2 * i / seg;
            double hx = 16.0 * Math.pow(Math.sin(t), 3.0);
            double hz = -(13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t));
            pts.add(new double[]{hx * factor, hz * factor});
        }
        return pts;
    }

    public static List<double[]> scatter(int range, int clusters) {
        List<double[]> pts = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int c = 0; c < clusters; c++) {
            double ox = (rng.nextDouble() - 0.5) * 2.0 * range;
            double oz = (rng.nextDouble() - 0.5) * 2.0 * range;
            pts.add(new double[]{ox, oz});
            for (int i = 0; i < 5; i++) {
                double a = rng.nextDouble() * 2.0 * Math.PI;
                double r = rng.nextDouble() * 5.0;
                pts.add(new double[]{ox + Math.cos(a) * r, oz + Math.sin(a) * r});
            }
        }
        return pts;
    }

    public static List<double[]> grid(int scale, int gap) {
        List<double[]> pts = new ArrayList<>();
        for (int gx = -scale; gx <= scale; gx += gap) {
            for (int gz = -scale; gz <= scale; gz += gap) {
                pts.add(new double[]{gx, gz});
            }
        }
        return pts;
    }

    /** Rasterises the text with a bold 12px font; each dark pixel becomes one point, scaled and centred. */
    public static List<double[]> text(String text, int scale) {
        List<double[]> pts = new ArrayList<>();
        BufferedImage measureImg = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D measure = measureImg.createGraphics();
        measure.setFont(TEXT_FONT);
        FontMetrics fm = measure.getFontMetrics();
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        measure.dispose();
        if (width <= 0 || height <= 0) {
            return pts;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setFont(TEXT_FONT);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.drawString(text, 0, fm.getAscent());
        g.dispose();

        double halfW = width * scale / 2.0;
        double halfH = height * scale / 2.0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if ((img.getRGB(col, row) & 0xFFFFFF) == 0) {
                    pts.add(new double[]{col * scale - halfW, row * scale - halfH});
                }
            }
        }
        return pts;
    }
}
