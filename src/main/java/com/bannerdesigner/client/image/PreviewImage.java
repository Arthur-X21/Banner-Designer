package com.bannerdesigner.client.image;

import java.awt.image.BufferedImage;

public class PreviewImage {
    private final String name;
    private final BufferedImage image;

    public PreviewImage(String name, BufferedImage image) {
        this.name = name;
        this.image = image;
    }

    public String name() { return name; }
    public BufferedImage image() { return image; }
    public int width() { return image.getWidth(); }
    public int height() { return image.getHeight(); }
}
