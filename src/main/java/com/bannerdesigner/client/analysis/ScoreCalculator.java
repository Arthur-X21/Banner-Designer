package com.bannerdesigner.client.analysis;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class ScoreCalculator {
    private ScoreCalculator() {}

    public static double score(BufferedImage target, BufferedImage rendered) {
        BufferedImage t = downscale(target, 32);
        BufferedImage r = downscale(rendered, 32);
        if (t == null || r == null) return 0.0;
        return 0.40 * colorSimilarity(t, r)
             + 0.35 * shapeSimilarity(t, r)
             + 0.25 * regionSimilarity(t, r);
    }

    private static BufferedImage downscale(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    private static double colorSimilarity(BufferedImage a, BufferedImage b) {
        int[] h1 = histogram(a), h2 = histogram(b);
        long inter = 0;
        for (int i = 0; i < h1.length; i++) inter += Math.min(h1[i], h2[i]);
        return (double) inter / (a.getWidth() * a.getHeight());
    }

    private static int[] histogram(BufferedImage img) {
        int[] h = new int[4096];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = ((rgb >> 16) & 0xFF) >> 4;
                int g = ((rgb >> 8) & 0xFF) >> 4;
                int b = (rgb & 0xFF) >> 4;
                h[(r << 8) | (g << 4) | b]++;
            }
        }
        return h;
    }

    private static double shapeSimilarity(BufferedImage a, BufferedImage b) {
        int w = a.getWidth(), h = a.getHeight();
        long diff = 0;
        long max = (long) w * h * 3 * 255;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ca = a.getRGB(x, y), cb = b.getRGB(x, y);
                diff += Math.abs(((ca >> 16) & 0xFF) - ((cb >> 16) & 0xFF));
                diff += Math.abs(((ca >> 8) & 0xFF) - ((cb >> 8) & 0xFF));
                diff += Math.abs((ca & 0xFF) - (cb & 0xFF));
            }
        }
        return 1.0 - ((double) diff / max);
    }

    private static double regionSimilarity(BufferedImage a, BufferedImage b) {
        int w = a.getWidth(), h = a.getHeight();
        int halfW = w / 2, halfH = h / 2;
        double total = 0;
        total += dist(quad(a, 0, 0, halfW, halfH), quad(b, 0, 0, halfW, halfH));
        total += dist(quad(a, halfW, 0, w-halfW, halfH), quad(b, halfW, 0, w-halfW, halfH));
        total += dist(quad(a, 0, halfH, halfW, h-halfH), quad(b, 0, halfH, halfW, h-halfH));
        total += dist(quad(a, halfW, halfH, w-halfW, h-halfH), quad(b, halfW, halfH, w-halfW, h-halfH));
        return 1.0 - (total / 4.0 / (255.0 * Math.sqrt(3)));
    }

    private static double[] quad(BufferedImage img, int x0, int y0, int w, int h) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                int c = img.getRGB(x, y);
                r += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF; b += c & 0xFF; n++;
            }
        }
        if (n == 0) n = 1;
        return new double[]{ r/(double)n, g/(double)n, b/(double)n };
    }

    private static double dist(double[] a, double[] b) {
        double dr = a[0]-b[0], dg = a[1]-b[1], db = a[2]-b[2];
        return Math.sqrt(dr*dr + dg*dg + db*db);
    }
}
