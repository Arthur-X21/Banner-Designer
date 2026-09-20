package com.bannerdesigner.client.inventory;

import net.minecraft.item.Item;

import java.util.List;

public record ResourceReport(
        List<Requirement> required,
        List<Requirement> owned,
        List<Requirement> missing
) {
    public record Requirement(Item item, int count, String display) {}
}
