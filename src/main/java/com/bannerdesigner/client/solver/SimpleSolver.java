package com.bannerdesigner.client.solver;

import com.bannerdesigner.client.analysis.AnalysisResult;
import com.bannerdesigner.client.analysis.ColorAnalyzer;
import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import com.bannerdesigner.client.banner.BannerPatternInfo;
import com.bannerdesigner.client.banner.BannerRenderer2D;
import net.minecraft.util.DyeColor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SimpleSolver {

    private SimpleSolver() {}

    public static List<BannerCandidate> solve(BufferedImage target, AnalysisResult analysis, int maxCandidates) {
        List<BannerCandidate> candidates = new ArrayList<>();
        List<DyeColor> colors = analysis.dominantColors();

        DyeColor base = colors.get(0);

        for (String pattern : BannerPatternInfo.COMMON_PATTERNS) {
            for (int i = 1; i < Math.min(colors.size(), 4); i++) {
                DyeColor layerColor = colors.get(i);
                if (layerColor == base) continue;

                BannerLayer layer = new BannerLayer(pattern, layerColor, 0);
                BannerDefinition def = new BannerDefinition(base, List.of(layer));

                BufferedImage rendered = BannerRenderer2D.render(def);
                double score = ColorAnalyzer.similarity(target, rendered);
                candidates.add(new BannerCandidate(def, score));
            }
        }

        if (colors.size() >= 2) {
            DyeColor c1 = colors.get(1);
            for (String pattern1 : new String[]{"stripe_bottom", "stripe_top", "border", "circle", "creeper"}) {
                BannerDefinition def = new BannerDefinition(base,
                        List.of(new BannerLayer(pattern1, c1, 0)));
                BufferedImage rendered = BannerRenderer2D.render(def);
                double score = ColorAnalyzer.similarity(target, rendered);
                candidates.add(new BannerCandidate(def, score));
            }
        }

        BannerDefinition baseOnly = new BannerDefinition(base, List.of());
        BufferedImage baseRendered = BannerRenderer2D.render(baseOnly);
        candidates.add(new BannerCandidate(baseOnly, ColorAnalyzer.similarity(target, baseRendered)));

        candidates.sort(Comparator.comparingDouble(BannerCandidate::score).reversed());

        return candidates.size() > maxCandidates
                ? candidates.subList(0, maxCandidates)
                : candidates;
    }
}
