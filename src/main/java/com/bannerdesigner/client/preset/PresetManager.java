package com.bannerdesigner.client.preset;

import com.bannerdesigner.client.BannerDesignerClient;
import com.bannerdesigner.util.BannerDesignerPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class PresetManager {

    private static final List<PresetEntry> PRESETS = new ArrayList<>();

    private PresetManager() {}

    public static void reload() {
        List<PresetEntry> newList = new ArrayList<>();
        Path dir = BannerDesignerPaths.PRESETS;

        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile)
                      .filter(PresetManager::isSupportedImage)
                      .sorted((a, b) -> a.getFileName().toString()
                              .compareToIgnoreCase(b.getFileName().toString()))
                      .forEach(p -> newList.add(new PresetEntry(
                              p.getFileName().toString(),
                              p
                      )));
            } catch (IOException e) {
                BannerDesignerClient.LOGGER.error("Failed to scan presets directory", e);
            }
        }

        synchronized (PRESETS) {
            PRESETS.clear();
            PRESETS.addAll(newList);
        }
    }

    public static List<PresetEntry> snapshot() {
        synchronized (PRESETS) {
            return List.copyOf(PRESETS);
        }
    }

    private static boolean isSupportedImage(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".png")
            || name.endsWith(".jpg")
            || name.endsWith(".jpeg");
    }
}
