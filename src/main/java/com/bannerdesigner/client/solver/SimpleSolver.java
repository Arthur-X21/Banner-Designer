package com.bannerdesigner.client.solver;

import com.bannerdesigner.client.analysis.AnalysisResult;
import com.bannerdesigner.client.analysis.ColorAnalyzer;
import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import com.bannerdesigner.client.banner.BannerPatternInfo;
import com.bannerdesigner.client.banner.BannerRenderer2D;
import com.bannerdesigner.client.config.ConfigManager;
import net.minecraft.util.DyeColor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SimpleSolver {

    private SimpleSolver() {}

    public static List<BannerCandidate> solve(BufferedImage target, AnalysisResult analysis,
                                              int maxCandidates) {
        List<BannerCandidate> pool = new ArrayList<>();
        List<DyeColor> colors = analysis.dominantColors();
        if (colors.isEmpty()) colors.add(DyeColor.WHITE);

        int maxLayers = ConfigManager.solverMaxLayers();

        // Single-layer search
        for (DyeColor base : colors.subList(0, Math.min(2, colors.size()))) {
            BannerDefinition baseDef = new BannerDefinition(base, List.of());
            pool.add(new BannerCandidate(baseDef, score(target, baseDef)));

            for (String pattern : BannerPatternInfo.COMMON_PATTERNS) {
                for (int i = 0; i < colors.size(); i++) {
                    DyeColor lc = colors.get(i);
                    if (lc == base) continue;
                    BannerDefinition def = new BannerDefinition(base,
                            List.of(new BannerLayer(pattern, lc, 0)));
                    pool.add(new BannerCandidate(def, score(target, def)));
                }
            }
        }

        pool.sort(Comparator.comparingDouble(BannerCandidate::score).reversed());

        // Multi-layer expansion from top candidates
        if (maxLayers > 1) {
            List<BannerCandidate> top = new ArrayList<>(pool.subList(0, Math.min(10, pool.size())));
            for (BannerCandidate c : top) {
                BannerDefinition baseDef = c.definition();
                if (baseDef.layerCount() >= maxLayers) continue;

                for (String pattern : BannerPatternInfo.COMMON_PATTERNS) {
                    for (DyeColor lc : colors.subList(0, Math.min(3, colors.size()))) {
                        BannerDefinition extended = baseDef.withLayer(
                                new BannerLayer(pattern, lc, baseDef.layerCount()));
                        if (extended.layerCount() > baseDef.layerCount()) {
                            pool.add(new BannerCandidate(extended, score(target, extended)));
                        }
                    }
                }
            }
        }

        pool.sort(Comparator.comparingDouble(BannerCandidate::score).reversed());

        // Deduplicate by definition string
        List<BannerCandidate> unique = new ArrayList<>();
        for (BannerCandidate c : pool) {
            boolean dup = false;
            for (BannerCandidate u : unique) {
                if (sameDef(u.definition(), c.definition())) { dup = true; break; }
            }
            if (!dup) unique.add(c);
            if (unique.size() >= maxCandidates) break;
        }
        return unique;
    }

    private static double score(BufferedImage target, BannerDefinition def) {
        BufferedImage rendered = BannerRenderer2D.render(def);
        return ColorAnalyzer.similarity(target, rendered);
    }

    private static boolean sameDef(BannerDefinition a, BannerDefinition b) {
        if (a.baseColor() != b.baseColor()) return false;
        if (a.layerCount() != b.layerCount()) return false;
        for (int i = 0; i < a.layerCount(); i++) {
            BannerLayer la = a.layers().get(i);
            BannerLayer lb = b.layers().get(i);
            if (!la.patternId().equals(lb.patternId())) return false;
            if (la.color() != lb.color()) return false;
        }
        return true;
    }
}
