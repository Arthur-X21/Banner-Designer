package com.bannerdesigner.client.cache;

import com.bannerdesigner.client.BannerDesignerClient;
import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import com.bannerdesigner.client.config.ConfigManager;
import com.bannerdesigner.client.solver.BannerCandidate;
import com.bannerdesigner.util.BannerDesignerPaths;
import net.minecraft.util.DyeColor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public final class CacheManager {

    private CacheManager() {}

    public static String hashBytes(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static Path cacheFile(String hash) {
        return BannerDesignerPaths.CACHE.resolve(hash + ".txt");
    }

    public static List<BannerCandidate> load(String hash) {
        if (!ConfigManager.cacheEnabled() || hash == null) return null;
        Path f = cacheFile(hash);
        if (!Files.exists(f)) return null;

        try (BufferedReader r = Files.newBufferedReader(f)) {
            String header = r.readLine();
            if (header == null || !header.startsWith("v1|")) return null;

            List<BannerCandidate> out = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) {
                BannerCandidate c = parseCandidate(line);
                if (c != null) out.add(c);
            }
            return out;
        } catch (Exception e) {
            BannerDesignerClient.LOGGER.warn("Cache read failed", e);
            return null;
        }
    }

    public static void save(String hash, List<BannerCandidate> candidates) {
        if (!ConfigManager.cacheEnabled() || hash == null) return;

        try {
            Files.createDirectories(BannerDesignerPaths.CACHE);
            Path f = cacheFile(hash);
            try (BufferedWriter w = Files.newBufferedWriter(f)) {
                w.write("v1|banner-designer");
                w.newLine();
                for (BannerCandidate c : candidates) {
                    w.write(serializeCandidate(c));
                    w.newLine();
                }
            }
        } catch (IOException e) {
            BannerDesignerClient.LOGGER.warn("Cache write failed", e);
        }
    }

    private static String serializeCandidate(BannerCandidate c) {
        BannerDefinition d = c.definition();
        StringBuilder sb = new StringBuilder();
        sb.append(d.baseColor().name().toLowerCase()).append("|").append(c.score());
        for (BannerLayer layer : d.layers()) {
            sb.append("|").append(layer.patternId())
              .append(":").append(layer.color().name().toLowerCase());
        }
        return sb.toString();
    }

    private static BannerCandidate parseCandidate(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length < 2) return null;
            DyeColor base = com.bannerdesigner.client.banner.DyeColorHelper.byId(parts[0], DyeColor.WHITE);
            double score = Double.parseDouble(parts[1]);
            List<BannerLayer> layers = new ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                String[] p = parts[i].split(":");
                if (p.length != 2) continue;
                DyeColor color = com.bannerdesigner.client.banner.DyeColorHelper.byId(p[1], DyeColor.WHITE);
                layers.add(new BannerLayer(p[0], color, i - 2));
            }
            return new BannerCandidate(new BannerDefinition(base, layers), score);
        } catch (Exception e) {
            return null;
        }
    }
}
