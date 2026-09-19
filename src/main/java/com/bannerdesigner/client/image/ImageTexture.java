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

    private static final int MAX_TEXTURE_SIZE = 512;

    private final Identifier identifier;
    private final int width;
    private final int height;

    private ImageTexture(Identifier identifier, int width, int height) {
        this.identifier = identifier;
        this.width = width;
        this.height = height;
    }

    public static ImageTexture fromBufferedImage(String name, BufferedImage source) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();

        float ratio = Math.min(1.0f,
                Math.min((float) MAX_TEXTURE_SIZE / srcW, (float) MAX_TEXTURE_SIZE / srcH));
        int targetW = Math.max(1, (int) (srcW * ratio));
        int targetH = Math.max(1, (int) (srcH * ratio));

        BufferedImage scaled;
        if (targetW == srcW && targetH == srcH) {
            scaled = source;
        } else {
            scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, targetW, targetH, null);
            g.dispose();
        }

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
        tex.setFilter(true, false);

        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);

        return new ImageTexture(id, targetW, targetH);
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
