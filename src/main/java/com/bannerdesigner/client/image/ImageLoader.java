package com.bannerdesigner.client.image;

import com.bannerdesigner.client.BannerDesignerClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class ImageLoader {
    private ImageLoader() {}

    public static BufferedImage load(Path path) {
        try {
            BufferedImage img = ImageIO.read(path.toFile());
            if (img == null) {
                BannerDesignerClient.LOGGER.warn("Unsupported image format: {}", path);
            }
            return img;
        } catch (IOException e) {
            BannerDesignerClient.LOGGER.error("Failed to load image: {}", path, e);
            return null;
        }
    }
}
