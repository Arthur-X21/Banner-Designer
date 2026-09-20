package com.bannerdesigner.client.util;

import com.bannerdesigner.client.BannerDesignerClient;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BackgroundExecutor {

    private static final ExecutorService WORKER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "banner-designer-worker");
                t.setDaemon(true);
                return t;
            });

    private BackgroundExecutor() {}

    public static <T> void submit(Supplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        WORKER.submit(() -> {
            try {
                T result = task.get();
                MinecraftClient.getInstance().execute(() -> {
                    try { onSuccess.accept(result); }
                    catch (Exception e) { BannerDesignerClient.LOGGER.warn("UI update failed", e); }
                });
            } catch (Throwable t) {
                BannerDesignerClient.LOGGER.warn("Background task failed", t);
                MinecraftClient.getInstance().execute(() -> {
                    try { onError.accept(t); }
                    catch (Exception ignored) {}
                });
            }
        });
    }

    public static void shutdown() {
        WORKER.shutdown();
        try { WORKER.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) {}
    }
}
