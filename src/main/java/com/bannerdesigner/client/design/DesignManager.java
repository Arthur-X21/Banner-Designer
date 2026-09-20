package com.bannerdesigner.client.design;

import com.bannerdesigner.client.BannerDesignerClient;
import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import com.bannerdesigner.client.banner.DyeColorHelper;
import com.bannerdesigner.util.BannerDesignerPaths;
import net.minecraft.util.DyeColor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DesignManager {
    private DesignManager() {}

    public static Path designPath(String name) {
        String safe = name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        return BannerDesignerPaths.DESIGNS.resolve(safe + ".bdesign");
    }

    public static void save(String name, BannerDefinition def, double score) {
        try {
            Files.createDirectories(BannerDesignerPaths.DESIGNS);
            try (BufferedWriter w = Files.newBufferedWriter(designPath(name))) {
                w.write("banner-design-v1"); w.newLine();
                w.write("score=" + score); w.newLine();
                w.write("base=" + def.baseColor().name()); w.newLine();
                for (BannerLayer layer : def.layers()) {
                    w.write("layer=" + layer.patternId() + ":" + layer.color().name());
                    w.newLine();
                }
            }
        } catch (IOException e) {
            BannerDesignerClient.LOGGER.warn("Failed to save design", e);
        }
    }

    public static LoadedDesign load(String name) {
        Path p = designPath(name);
        if (!Files.exists(p)) return null;
        try (BufferedReader r = Files.newBufferedReader(p)) {
            if (!"banner-design-v1".equals(r.readLine())) return null;
            double score = 0;
            DyeColor base = DyeColor.WHITE;
            List<BannerLayer> layers = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("score=")) {
                    try { score = Double.parseDouble(line.substring(6)); } catch (Exception ignored) {}
                } else if (line.startsWith("base=")) {
                    base = DyeColorHelper.byId(line.substring(5), DyeColor.WHITE);
                } else if (line.startsWith("layer=")) {
                    String[] p2 = line.substring(6).split(":");
                    if (p2.length == 2) {
                        layers.add(new BannerLayer(p2[0],
                                DyeColorHelper.byId(p2[1], DyeColor.WHITE), layers.size()));
                    }
                }
            }
            return new LoadedDesign(new BannerDefinition(base, layers), score);
        } catch (IOException e) { return null; }
    }

    public record LoadedDesign(BannerDefinition definition, double score) {}
}
