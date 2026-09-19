package com.bannerdesigner.client.solver;

import com.bannerdesigner.client.banner.BannerDefinition;

public record BannerCandidate(BannerDefinition definition, double score) {
}
