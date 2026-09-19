package com.bannerdesigner.client.config;

import com.bannerdesigner.client.BannerDesignerClient;
import com.bannerdesigner.util.BannerDesignerPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigManager {

    private static final Properties PROPS = new Properties();
    private static final Path CONFIG_FILE = BannerDesignerPaths.ROOT.resolve("config.properties");

    private static int maxCandidates = 3;
    private static int solverMaxLayers = 3;
    private static boolean cacheEnabled = true;

    private ConfigManager() {}

    public static void load() {
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                PROPS.load(in);
            } catch (IOException e) {
                BannerDesignerClient.LOGGER.warn("Failed to load config", e);
            }
        } else {
            PROPS.setProperty("maxCandidates", "3");
            PROPS.setProperty("solverMaxLayers", "3");
            PROPS.setProperty("cacheEnabled", "true");
            save();
        }
        read();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                PROPS.store(out, "Banner Designer Configuration");
            }
        } catch (IOException e) {
            BannerDesignerClient.LOGGER.warn("Failed to save config", e);
        }
    }

    private static void read() {
        maxCandidates = parseInt(PROPS.getProperty("maxCandidates"), 3);
        solverMaxLayers = parseInt(PROPS.getProperty("solverMaxLayers"), 3);
        cacheEnabled = Boolean.parseBoolean(PROPS.getProperty("cacheEnabled", "true"));
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public static int maxCandidates() { return maxCandidates; }
    public static int solverMaxLayers() { return solverMaxLayers; }
    public static boolean cacheEnabled() { return cacheEnabled; }
}
