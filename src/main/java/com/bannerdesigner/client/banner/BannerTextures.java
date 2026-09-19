package com.bannerdesigner.client.banner;

import com.bannerdesigner.client.BannerDesignerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BannerTextures {

    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    private BannerTextures() {}

    public static BufferedImage base() {
        return get("base");
    }

    public static BufferedImage pattern(String patternId) {
        return get(patternId);
    }

    private static BufferedImage get(String name) {
        BufferedImage cached = CACHE.get(name);
        if (cached != null) return cached;

        BufferedImage loaded = tryLoad(name);
        if (loaded != null) CACHE.put(name, loaded);
        return loaded;
    }

    private static BufferedImage tryLoad(String name) {
        String[] candidatePaths = {
                "textures/entity/banner/" + name + ".png",
                "textures/entity/banner_pattern/" + name + ".png",
        };

        ResourceManager rm;
        try {
            rm = MinecraftClient.getInstance().getResourceManager();
        } catch (Exception e) {
            return null;
        }

        for (String path : candidatePaths) {
            try {
                Identifier id = Identifier.of("minecraft", path);
                Optional<Resource> opt = rm.getResource(id);
                if (opt.isEmpty()) continue;

                try (InputStream in = opt.get().getInputStream()) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) return img;
                }
            } catch (Exception ignored) {
                // try next path
            }
        }
        BannerDesignerClient.LOGGER.warn("Could not load banner texture: {}", name);
        return null;
    }

    public static void clear() {
        CACHE.clear();
    }
}
