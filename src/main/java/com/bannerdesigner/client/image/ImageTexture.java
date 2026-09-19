package com.bannerdesigner.client.image;

import com.bannerdesigner.client.BannerDesignerClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

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

    public static ImageTexture fromBufferedImage(String name, BufferedImage buffered) {
        int w = buffered.getWidth();
        int h = buffered.getHeight();

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                nativeImage.setColor(x, y, buffered.getRGB(x, y));
            }
        }

        String safeName = name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        Identifier id = Identifier.of("banner-designer", "dynamic/" + safeName + "_" + System.nanoTime());

        NativeImageBackedTexture tex = new NativeImageBackedTexture(nativeImage);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);

        return new ImageTexture(id, w, h);
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
