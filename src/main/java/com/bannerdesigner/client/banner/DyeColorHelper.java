package com.bannerdesigner.client.banner;

import net.minecraft.util.DyeColor;

public final class DyeColorHelper {
    private DyeColorHelper() {}

    public static int rgb(DyeColor color) {
        switch (color) {
            case WHITE:      return 0xF9FFFE;
            case ORANGE:     return 0xF9801D;
            case MAGENTA:    return 0xC74EBD;
            case LIGHT_BLUE: return 0x3AB3DA;
            case YELLOW:     return 0xFED83D;
            case LIME:       return 0x80C71F;
            case PINK:       return 0xF38BAA;
            case GRAY:       return 0x474F52;
            case LIGHT_GRAY: return 0x9D9D97;
            case CYAN:       return 0x169C9C;
            case PURPLE:     return 0x8932B8;
            case BLUE:       return 0x3C44AA;
            case BROWN:      return 0x835432;
            case GREEN:      return 0x5E7C16;
            case RED:        return 0xB02E26;
            case BLACK:      return 0x1D1D21;
            default:         return 0xFFFFFF;
        }
    }

    public static int argb(DyeColor color) {
        return 0xFF000000 | rgb(color);
    }

    public static DyeColor nearest(int r, int g, int b) {
        DyeColor best = DyeColor.WHITE;
        long bestDist = Long.MAX_VALUE;
        for (DyeColor c : DyeColor.values()) {
            int rgb = rgb(c);
            int cr = (rgb >> 16) & 0xFF;
            int cg = (rgb >> 8) & 0xFF;
            int cb = rgb & 0xFF;
            long dr = r - cr, dg = g - cg, db = b - cb;
            long dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    public static DyeColor nearest(int argb) {
        return nearest((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    /** Safe replacement for DyeColor.getName() across versions. */
    public static String id(DyeColor color) {
        return color.name().toLowerCase();
    }

    /** Safe replacement for DyeColor.byName(String, DyeColor). */
    public static DyeColor byId(String id, DyeColor fallback) {
        if (id == null) return fallback;
        for (DyeColor c : DyeColor.values()) {
            if (c.name().equalsIgnoreCase(id)) return c;
        }
        return fallback;
    }
}
