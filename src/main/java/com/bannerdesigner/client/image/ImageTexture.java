package com.bannerdesigner.client.image;

import com.bannerdesigner.client.BannerDesignerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ImageTexture implements AutoCloseable {

    private final Identifier identifier;
    private final int width;
    private final int height;

    private ImageTexture(Identifier identifier, int width, int height) {
        this.identifier = identifier;
        this.width = width;
        this.height = height;
    }

    public static ImageTexture fromBufferedImage(String name, BufferedImage source,
                                                  int maxWidth, int maxHeight) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();

        float scale = Math.min(
                (float) maxWidth / srcW,
                (float) maxHeight / srcH
        );
        if (scale > 1.0f) scale = 1.0f;
        int targetW = Math.max(1, (int) (srcW * scale));
        int targetH = Math.max(1, (int) (srcH * scale));

        BufferedImage scaled = smoothDownscale(source, targetW, targetH);

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, targetW, targetH, false);
        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                nativeImage.setColor(x, y, scaled.getRGB(x, y));
            }
        }

        String safeName = name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        Identifier id = Identifier.of("banner-designer",
                "dynamic/" + safeName + "_" + System.nanoTime());

        NativeImageBackedTexture tex = new NativeImageBackedTexture(
                () -> "banner_designer_" + safeName,
                nativeImage
        );
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);

        return new ImageTexture(id, targetW, targetH);
    }

    /**
     * Progressive downscale: repeatedly halve the image until it is close to
     * the target size, then do a final resize. This preserves far more detail
     * than a single-pass downscale.
     */
    private static BufferedImage smoothDownscale(BufferedImage src, int targetW, int targetH) {
        BufferedImage current = src;
        while (current.getWidth() / 2 >= targetW && current.getHeight() / 2 >= targetH) {
            int halfW = current.getWidth() / 2;
            int halfH = current.getHeight() / 2;
            BufferedImage half = new BufferedImage(halfW, halfH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = half.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, halfW, halfH, null);
            g.dispose();
            current = half;
        }

        if (current.getWidth() == targetW && current.getHeight() == targetH) {
            return current;
        }

        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(current, 0, 0, targetW, targetH, null);
        g.dispose();
        return out;
    }

    public Identifier identifier() { return identifier; }
    public int width() { return width; }
    public int height() { return height; }

    @Override
    public void close() {
        try {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier);
        } catch (Exception e) {
            BannerDesignerClient.LOGGER.warn("Failed to destroy texture {}", identifier, e);
        }
    }
}
