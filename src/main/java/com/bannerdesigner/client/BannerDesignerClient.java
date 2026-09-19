package com.bannerdesigner.client;

import com.bannerdesigner.client.config.ConfigManager;
import com.bannerdesigner.util.BannerDesignerPaths;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BannerDesignerClient implements ClientModInitializer {
    public static final String MOD_ID = "banner-designer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Banner Designer initializing...");
        BannerDesignerPaths.ensureDirectories();
        ConfigManager.load();
        LOGGER.info("Banner Designer initialized.");
    }
}
