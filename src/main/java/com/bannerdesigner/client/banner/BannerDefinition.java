package com.bannerdesigner.client.banner;

import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.List;

public class BannerDefinition {
    public static final int MAX_LAYERS = 6;

    private final DyeColor baseColor;
    private final List<BannerLayer> layers;

    public BannerDefinition(DyeColor baseColor, List<BannerLayer> layers) {
        this.baseColor = baseColor;
        this.layers = new ArrayList<>(layers);
        if (this.layers.size() > MAX_LAYERS) {
            this.layers.subList(MAX_LAYERS, this.layers.size()).clear();
        }
    }

    public DyeColor baseColor() { return baseColor; }
    public List<BannerLayer> layers() { return List.copyOf(layers); }
    public int layerCount() { return layers.size(); }

    public BannerDefinition withLayer(BannerLayer layer) {
        List<BannerLayer> copy = new ArrayList<>(layers);
        if (copy.size() < MAX_LAYERS) {
            copy.add(layer);
        }
        return new BannerDefinition(baseColor, copy);
    }

    public boolean isValid() {
        return baseColor != null && layers.size() <= MAX_LAYERS;
    }
}
