package com.bannerdesigner.client.banner;

import net.minecraft.util.DyeColor;

public record BannerLayer(String patternId, DyeColor color, int order) {
}
