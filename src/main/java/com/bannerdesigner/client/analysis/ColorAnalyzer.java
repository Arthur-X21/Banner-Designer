package com.bannerdesigner.client.analysis;

import net.minecraft.util.DyeColor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ColorAnalyzer {

    private static final int SAMPLE_SIZE = 32;

    private ColorAnalyzer() {}

    public static AnalysisResult analyze(BufferedImage source) {
        BufferedImage small = new BufferedImage(SAMPLE_SIZE, SAMPLE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = small.createGraphics();
        g.drawImage(source, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE, null);
        g.dispose();

        Map<Integer, Integer> histogram = new HashMap<>();
        for (int y = 0; y < SAMPLE_SIZE; y++) {
            for (int x = 0; x < SAMPLE_SIZE; x++) {
                int argb = small.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 128) continue;

                int r = (argb >> 16) & 0xFF;
                int gg = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                int qr = (r / 32) * 32;
                int qg = (gg / 32) * 32;
                int qb = (b / 32) * 32;
                int key = (qr << 16) | (qg << 8) | qb;

                histogram.merge(key, 1, Integer::sum);
            }
        }

        List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(histogram.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<Integer, Integer> e) -> e.getValue()).reversed());

        List<DyeColor> dominantColors = new ArrayList<>();
        int totalPixels = SAMPLE_SIZE * SAMPLE_SIZE;

        for (Map.Entry<Integer, Integer> e : sorted) {
            int key = e.getKey();
            int r = (key >> 16) & 0xFF;
            int gg = (key >> 8) & 0xFF;
            int b = key & 0xFF;
            DyeColor dye = com.bannerdesigner.client.banner.DyeColorHelper.nearest(r, gg, b);
            if (!dominantColors.contains(dye)) {
                dominantColors.add(dye);
            }
            if (dominantColors.size() >= 6) break;
        }

        if (dominantColors.isEmpty()) {
            dominantColors.add(DyeColor.WHITE);
        }

        return new AnalysisResult(dominantColors, totalPixels);
    }

    public static double similarity(BufferedImage a, BufferedImage b) {
        int size = 24;
        BufferedImage sa = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BufferedImage sb = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D ga = sa.createGraphics();
        ga.drawImage(a, 0, 0, size, size, null);
        ga.dispose();
        Graphics2D gb = sb.createGraphics();
        gb.drawImage(b, 0, 0, size, size, null);
        gb.dispose();

        long diff = 0;
        long max = (long) size * size * 3 * 255;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int ca = sa.getRGB(x, y);
                int cb = sb.getRGB(x, y);
                diff += Math.abs(((ca >> 16) & 0xFF) - ((cb >> 16) & 0xFF));
                diff += Math.abs(((ca >> 8) & 0xFF) - ((cb >> 8) & 0xFF));
                diff += Math.abs((ca & 0xFF) - (cb & 0xFF));
            }
        }
        return 1.0 - ((double) diff / max);
    }
}
