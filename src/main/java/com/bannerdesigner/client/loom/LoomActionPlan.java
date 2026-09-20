package com.bannerdesigner.client.loom;

import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import com.bannerdesigner.client.inventory.InventoryScanner;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public final class LoomActionPlan {

    private final BannerDefinition definition;
    private final List<LoomStep> steps;
    private int currentStep = 0;

    private LoomActionPlan(BannerDefinition definition, List<LoomStep> steps) {
        this.definition = definition;
        this.steps = steps;
    }

    public static LoomActionPlan build(BannerDefinition def) {
        List<LoomStep> steps = new ArrayList<>();
        int i = 1;

        steps.add(new LoomStep(i++, "Place base banner in top-left slot",
                itemName(InventoryScanner.bannerFor(def.baseColor())),
                LoomStep.StepType.INSERT_BASE_BANNER));

        for (BannerLayer layer : def.layers()) {
            steps.add(new LoomStep(i++, "Place dye in middle-left slot",
                    itemName(InventoryScanner.dyeFor(layer.color())),
                    LoomStep.StepType.INSERT_DYE));

            String patternDesc = "Select pattern: " + niceName(layer.patternId());
            Item patItem = InventoryScanner.patternItem(layer.patternId());
            String patternItem = "";
            if (patItem != null) {
                patternItem = itemName(patItem);
                patternDesc += " (requires " + patternItem + ")";
            } else {
                patternDesc += " (no item needed)";
            }
            steps.add(new LoomStep(i++, patternDesc, patternItem,
                    LoomStep.StepType.SELECT_PATTERN));

            steps.add(new LoomStep(i++, "Take result banner from output slot", "",
                    LoomStep.StepType.TAKE_RESULT));
        }

        steps.add(new LoomStep(i, "Done!", "", LoomStep.StepType.DONE));
        return new LoomActionPlan(def, steps);
    }

    public BannerDefinition definition() { return definition; }
    public List<LoomStep> steps() { return List.copyOf(steps); }
    public int currentStep() { return currentStep; }
    public int totalSteps() { return steps.size(); }
    public void next() { if (currentStep < steps.size() - 1) currentStep++; }
    public void previous() { if (currentStep > 0) currentStep--; }
    public void reset() { currentStep = 0; }
    public LoomStep current() { return steps.get(Math.min(currentStep, steps.size() - 1)); }

    private static String itemName(Item item) {
        try { return item.getName().getString(); }
        catch (Exception e) { return item.toString(); }
    }

    private static String niceName(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
