package com.bannerdesigner.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BannerDesignerPaths {
    private BannerDesignerPaths() {}

    public static final Path ROOT = FabricLoader.getInstance()
            .getGameDir()
            .resolve("bannerdesigner");

    public static final Path PRESETS = ROOT.resolve("presets");
    public static final Path IMPORT  = ROOT.resolve("import");
    public static final Path CACHE   = ROOT.resolve("cache");
    public static final Path DESIGNS = ROOT.resolve("designs");

    public static void ensureDirectories() {
        for (Path p : new Path[]{ROOT, PRESETS, IMPORT, CACHE, DESIGNS}) {
            try {
                Files.createDirectories(p);
            } catch (IOException e) {
                com.bannerdesigner.client.BannerDesignerClient.LOGGER
                        .error("Failed to create directory: {}", p, e);
            }
        }
    }
}
