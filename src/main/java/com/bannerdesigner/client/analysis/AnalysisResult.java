package com.bannerdesigner.client.analysis;

import net.minecraft.util.DyeColor;

import java.util.List;

public record AnalysisResult(List<DyeColor> dominantColors, int sampleCount) {
}
