package com.bannerdesigner.client.inventory;

import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InventoryScanner {
    private InventoryScanner() {}

    public static ResourceReport analyze(BannerDefinition def) {
        Map<Item, Integer> required = computeRequired(def);
        Map<Item, Integer> owned = readInventory();

        List<ResourceReport.Requirement> reqList = toList(required);
        List<ResourceReport.Requirement> ownedList = new ArrayList<>();
        List<ResourceReport.Requirement> missingList = new ArrayList<>();

        for (Map.Entry<Item, Integer> e : required.entrySet()) {
            Item item = e.getKey();
            int need = e.getValue();
            int have = owned.getOrDefault(item, 0);
            if (have > 0) ownedList.add(new ResourceReport.Requirement(
                    item, Math.min(have, need), displayName(item)));
            if (have < need) missingList.add(new ResourceReport.Requirement(
                    item, need - have, displayName(item)));
        }
        return new ResourceReport(reqList, ownedList, missingList);
    }

    private static Map<Item, Integer> computeRequired(BannerDefinition def) {
        Map<Item, Integer> map = new HashMap<>();
        map.merge(bannerFor(def.baseColor()), 1, Integer::sum);
        for (BannerLayer layer : def.layers()) {
            map.merge(dyeFor(layer.color()), 1, Integer::sum);
            Item pat = patternItem(layer.patternId());
            if (pat != null) map.merge(pat, 1, Integer::sum);
        }
        return map;
    }

    private static Map<Item, Integer> readInventory() {
        Map<Item, Integer> counts = new HashMap<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return counts;
        ClientPlayerEntity player = mc.player;
        PlayerInventory inv = player.getInventory();
        int size = inv.size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static List<ResourceReport.Requirement> toList(Map<Item, Integer> map) {
        List<ResourceReport.Requirement> list = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : map.entrySet())
            list.add(new ResourceReport.Requirement(e.getKey(), e.getValue(), displayName(e.getKey())));
        return list;
    }

    private static String displayName(Item item) {
        try { return item.getName().getString(); }
        catch (Exception e) { return item.toString(); }
    }

    public static Item bannerFor(DyeColor c) {
        switch (c) {
            case WHITE: return Items.WHITE_BANNER;
            case ORANGE: return Items.ORANGE_BANNER;
            case MAGENTA: return Items.MAGENTA_BANNER;
            case LIGHT_BLUE: return Items.LIGHT_BLUE_BANNER;
            case YELLOW: return Items.YELLOW_BANNER;
            case LIME: return Items.LIME_BANNER;
            case PINK: return Items.PINK_BANNER;
            case GRAY: return Items.GRAY_BANNER;
            case LIGHT_GRAY: return Items.LIGHT_GRAY_BANNER;
            case CYAN: return Items.CYAN_BANNER;
            case PURPLE: return Items.PURPLE_BANNER;
            case BLUE: return Items.BLUE_BANNER;
            case BROWN: return Items.BROWN_BANNER;
            case GREEN: return Items.GREEN_BANNER;
            case RED: return Items.RED_BANNER;
            case BLACK: return Items.BLACK_BANNER;
            default: return Items.WHITE_BANNER;
        }
    }

    public static Item dyeFor(DyeColor c) {
        switch (c) {
            case WHITE: return Items.WHITE_DYE;
            case ORANGE: return Items.ORANGE_DYE;
            case MAGENTA: return Items.MAGENTA_DYE;
            case LIGHT_BLUE: return Items.LIGHT_BLUE_DYE;
            case YELLOW: return Items.YELLOW_DYE;
            case LIME: return Items.LIME_DYE;
            case PINK: return Items.PINK_DYE;
            case GRAY: return Items.GRAY_DYE;
            case LIGHT_GRAY: return Items.LIGHT_GRAY_DYE;
            case CYAN: return Items.CYAN_DYE;
            case PURPLE: return Items.PURPLE_DYE;
            case BLUE: return Items.BLUE_DYE;
            case BROWN: return Items.BROWN_DYE;
            case GREEN: return Items.GREEN_DYE;
            case RED: return Items.RED_DYE;
            case BLACK: return Items.BLACK_DYE;
            default: return Items.WHITE_DYE;
        }
    }

    public static Item patternItem(String patternId) {
        switch (patternId) {
            case "creeper": return Items.CREEPER_BANNER_PATTERN;
            case "skull": return Items.SKULL_BANNER_PATTERN;
            case "globe": return Items.GLOBE_BANNER_PATTERN;
            case "piglin": return Items.PIGLIN_BANNER_PATTERN;
            case "flow": return Items.FLOW_BANNER_PATTERN;
            case "mojang": return Items.MOJANG_BANNER_PATTERN;
            default: return null;
        }
    }
}
