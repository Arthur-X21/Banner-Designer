package com.bannerdesigner.client.banner;

import java.awt.image.BufferedImage;

public final class BannerRenderer2D {

    /** Output dimensions preserve banner's real 1:2 aspect ratio (width:height). */
    public static final int RENDER_WIDTH = 40;
    public static final int RENDER_HEIGHT = 80;

    private BannerRenderer2D() {}

    public static BufferedImage render(BannerDefinition def) {
        BufferedImage base = BannerTextures.base();
        if (base != null) {
            return renderWithTextures(def, base);
        }
        return renderGeometricFallback(def);
    }

    private static BufferedImage renderWithTextures(BannerDefinition def, BufferedImage baseTex) {
        int srcW = baseTex.getWidth();
        int srcH = baseTex.getHeight();

        BufferedImage out = new BufferedImage(RENDER_WIDTH, RENDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        // Base layer
        int baseRgb = DyeColorHelper.rgb(def.baseColor());
        for (int y = 0; y < RENDER_HEIGHT; y++) {
            int sy = (y * srcH) / RENDER_HEIGHT;
            for (int x = 0; x < RENDER_WIDTH; x++) {
                int sx = (x * srcW) / RENDER_WIDTH;
                int src = baseTex.getRGB(sx, sy);
                out.setRGB(x, y, tint(src, baseRgb));
            }
        }

        // Pattern layers
        for (BannerLayer layer : def.layers()) {
            BufferedImage pat = BannerTextures.pattern(layer.patternId());
            if (pat == null) continue;

            int patW = pat.getWidth();
            int patH = pat.getHeight();
            int patRgb = DyeColorHelper.rgb(layer.color());

            for (int y = 0; y < RENDER_HEIGHT; y++) {
                int sy = (y * patH) / RENDER_HEIGHT;
                for (int x = 0; x < RENDER_WIDTH; x++) {
                    int sx = (x * patW) / RENDER_WIDTH;
                    int src = pat.getRGB(sx, sy);
                    int alpha = (src >>> 24) & 0xFF;
                    if (alpha == 0) continue;

                    int tinted = tint(src, patRgb);
                    int existing = out.getRGB(x, y);
                    out.setRGB(x, y, blend(existing, tinted, alpha));
                }
            }
        }

        return out;
    }

    private static int tint(int src, int tintRgb) {
        int a = (src >>> 24) & 0xFF;
        int r = (src >> 16) & 0xFF;
        int g = (src >> 8) & 0xFF;
        int b = src & 0xFF;

        int tr = (tintRgb >> 16) & 0xFF;
        int tg = (tintRgb >> 8) & 0xFF;
        int tb = tintRgb & 0xFF;

        int nr = r * tr / 255;
        int ng = g * tg / 255;
        int nb = b * tb / 255;

        return (a << 24) | (nr << 16) | (ng << 8) | nb;
    }

    private static int blend(int dst, int src, int srcAlpha) {
        int dr = (dst >> 16) & 0xFF;
        int dg = (dst >> 8) & 0xFF;
        int db = dst & 0xFF;

        int sr = (src >> 16) & 0xFF;
        int sg = (src >> 8) & 0xFF;
        int sb = src & 0xFF;

        float a = srcAlpha / 255.0f;
        int nr = (int) (sr * a + dr * (1 - a));
        int ng = (int) (sg * a + dg * (1 - a));
        int nb = (int) (sb * a + db * (1 - a));

        return 0xFF000000 | (nr << 16) | (ng << 8) | nb;
    }

    // ---------- Geometric fallback (if textures cannot be loaded) ----------

    private static BufferedImage renderGeometricFallback(BannerDefinition def) {
        BufferedImage img = new BufferedImage(RENDER_WIDTH, RENDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int baseArgb = 0xFF000000 | DyeColorHelper.rgb(def.baseColor());
        for (int y = 0; y < RENDER_HEIGHT; y++)
            for (int x = 0; x < RENDER_WIDTH; x++)
                img.setRGB(x, y, baseArgb);

        for (BannerLayer layer : def.layers()) {
            int argb = 0xFF000000 | DyeColorHelper.rgb(layer.color());
            drawGeometric(img, layer.patternId(), argb);
        }
        return img;
    }

    private static void drawGeometric(BufferedImage img, String p, int argb) {
        for (int y = 0; y < RENDER_HEIGHT; y++) {
            for (int x = 0; x < RENDER_WIDTH; x++) {
                if (geomMatch(p, x, y, RENDER_WIDTH, RENDER_HEIGHT)) img.setRGB(x, y, argb);
            }
        }
    }

    private static boolean geomMatch(String p, int x, int y, int w, int h) {
        double fx = (double) x / w, fy = (double) y / h;
        switch (p) {
            case "stripe_bottom":    return fy >= 0.66;
            case "stripe_top":       return fy <= 0.33;
            case "stripe_left":      return fx <= 0.33;
            case "stripe_right":     return fx >= 0.66;
            case "stripe_center":    return fx >= 0.33 && fx <= 0.66;
            case "stripe_middle":    return fy >= 0.33 && fy <= 0.66;
            case "small_stripes":    return ((x / 3) % 2) == 0;
            case "cross":            return Math.abs(fx - 0.5) <= 0.16 || Math.abs(fy - 0.5) <= 0.16;
            case "straight_cross":   return Math.abs(fx - 0.5) <= 0.08 || Math.abs(fy - 0.5) <= 0.08;
            case "triangle_bottom":  return fy >= 0.5;
            case "triangle_top":     return fy <= 0.5;
            case "diagonal_left":    return Math.abs(fy - (1 - fx)) < 0.12;
            case "diagonal_right":   return Math.abs(fy - fx) < 0.12;
            case "circle":           return Math.hypot(fx - 0.5, fy - 0.5) <= 0.32;
            case "rhombus":          return Math.abs(fx - 0.5) + Math.abs(fy - 0.5) <= 0.42;
            case "half_vertical":    return fx <= 0.5;
            case "half_horizontal":  return fy <= 0.5;
            case "bordure":          return fx <= 0.15 || fx >= 0.85 || fy <= 0.1 || fy >= 0.9;
            case "curly_border":     return fx <= 0.12 || fx >= 0.88 || fy <= 0.08 || fy >= 0.92;
            case "gradient":         return fy >= 0.5;
            case "gradient_up":      return fy <= 0.5;
            case "creeper":          return isCreeper(fx, fy);
            case "bricks":           return ((x / 4) + (y / 6)) % 2 == 0;
            case "skull":            return Math.hypot(fx - 0.5, fy - 0.45) <= 0.28;
            case "globe":            return Math.hypot(fx - 0.5, fy - 0.5) <= 0.35
                                          && Math.hypot(fx - 0.5, fy - 0.5) >= 0.15;
            case "piglin":           return fy >= 0.2 && fy <= 0.8 && fx >= 0.3 && fx <= 0.7;
            default:                 return false;
        }
    }

    private static boolean isCreeper(double fx, double fy) {
        if (fy < 0.3 || fy > 0.75) return false;
        if (fx < 0.25 || fx > 0.75) return false;
        if (fy < 0.4 && (fx < 0.35 || fx > 0.65)) return false;
        return true;
    }
}
