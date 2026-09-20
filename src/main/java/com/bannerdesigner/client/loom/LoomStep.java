package com.bannerdesigner.client.loom;

public record LoomStep(
        int index,
        String description,
        String itemDescription,
        StepType type
) {
    public enum StepType {
        INSERT_BASE_BANNER,
        INSERT_DYE,
        SELECT_PATTERN,
        TAKE_RESULT,
        DONE
    }
}
