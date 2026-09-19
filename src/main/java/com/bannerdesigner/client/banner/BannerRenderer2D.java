package com.bannerdesigner.client.banner;

import java.awt.image.BufferedImage;

public final class BannerRenderer2D {

    public static final int WIDTH = 40;
    public static final int HEIGHT = 60;

    private BannerRenderer2D() {}

    public static BufferedImage render(BannerDefinition def) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int baseArgb = 0xFF000000 | DyeColorHelper.rgb(def.baseColor());
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                img.setRGB(x, y, baseArgb);

        for (BannerLayer layer : def.layers()) {
            int argb = 0xFF000000 | DyeColorHelper.rgb(layer.color());
            drawPattern(img, layer.patternId(), argb);
        }
        return img;
    }

    private static void drawPattern(BufferedImage img, String p, int argb) {
        int w = WIDTH, h = HEIGHT;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (matches(p, x, y, w, h)) img.setRGB(x, y, argb);
            }
        }
    }

    private static boolean matches(String p, int x, int y, int w, int h) {
        double fx = (double) x / w, fy = (double) y / h;
        switch (p) {
            case "stripe_bottom":       return fy >= 0.66;
            case "stripe_top":          return fy <= 0.33;
            case "stripe_left":         return fx <= 0.33;
            case "stripe_right":        return fx >= 0.66;
            case "stripe_center":       return fx >= 0.33 && fx <= 0.66;
            case "stripe_middle":       return fy >= 0.33 && fy <= 0.66;
            case "small_stripes":       return ((x / 3) % 2) == 0;
            case "cross":               return Math.abs(fx - 0.5) <= 0.16 || Math.abs(fy - 0.5) <= 0.16;
            case "straight_cross":      return Math.abs(fx - 0.5) <= 0.08 || Math.abs(fy - 0.5) <= 0.08;
            case "triangle_bottom":     return fy >= 0.5 - 0.3 * (0.5 - Math.abs(fx - 0.5)) * 2;
            case "triangle_top":        return fy <= 0.5 + 0.3 * (0.5 - Math.abs(fx - 0.5)) * 2;
            case "diagonal_left":       return Math.abs(fy - (1 - fx)) < 0.12;
            case "diagonal_right":      return Math.abs(fy - fx) < 0.12;
            case "circle":              return Math.hypot(fx - 0.5, fy - 0.5) <= 0.32;
            case "rhombus":             return Math.abs(fx - 0.5) + Math.abs(fy - 0.5) <= 0.42;
            case "half_vertical":       return fx <= 0.5;
            case "half_horizontal":     return fy <= 0.5;
            case "border":              return fx <= 0.15 || fx >= 0.85 || fy <= 0.1 || fy >= 0.9;
            case "curly_border":        return fx <= 0.12 || fx >= 0.88 || fy <= 0.08 || fy >= 0.92;
            case "gradient":            return fy >= 0.5;
            case "gradient_up":         return fy <= 0.5;
            case "creeper":             return isCreeper(fx, fy);
            case "bricks":              return ((x / 4) + (y / 6)) % 2 == 0;
            case "skull":               return Math.hypot(fx - 0.5, fy - 0.45) <= 0.28;
            case "flower":              return isFlower(fx, fy);
            case "mojang":              return isMojang(fx, fy);
            case "globe":               return Math.hypot(fx - 0.5, fy - 0.5) <= 0.35
                                            && Math.hypot(fx - 0.5, fy - 0.5) >= 0.15;
            case "piglin":              return fy >= 0.2 && fy <= 0.8 && fx >= 0.3 && fx <= 0.7;
            default:                    return false;
        }
    }

    private static boolean isCreeper(double fx, double fy) {
        if (fy < 0.3 || fy > 0.75) return false;
        if (fx < 0.25 || fx > 0.75) return false;
        if (fy < 0.4 && (fx < 0.35 || fx > 0.65)) return false;
        return true;
    }

    private static boolean isFlower(double fx, double fy) {
        double cx = 0.5, cy = 0.4;
        double dist = Math.hypot(fx - cx, fy - cy);
        return dist <= 0.15 || (fy > 0.4 && fy < 0.8 && Math.abs(fx - 0.5) < 0.05);
    }

    private static boolean isMojang(double fx, double fy) {
        return fy > 0.35 && fy < 0.65 && fx > 0.2 && fx < 0.8;
    }
}
