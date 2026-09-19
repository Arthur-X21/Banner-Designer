package com.bannerdesigner.client.banner;

public final class BannerPatternInfo {
    private BannerPatternInfo() {}

    /**
     * Names correspond to vanilla banner pattern texture files in:
     *   assets/minecraft/textures/entity/banner/<name>.png
     * These are the standard patterns available in Minecraft 1.21.x.
     */
    public static final String[] COMMON_PATTERNS = {
            // Stripes
            "stripe_bottom", "stripe_top", "stripe_left", "stripe_right",
            "stripe_center", "stripe_middle", "stripe_downright", "stripe_downleft",
            "small_stripes",
            // Crosses
            "cross", "straight_cross",
            // Triangles
            "triangle_bottom", "triangle_top", "triangles_bottom", "triangles_top",
            // Diagonals
            "diagonal_left", "diagonal_right", "diagonal_up_left", "diagonal_up_right",
            // Shapes
            "circle", "rhombus", "half_vertical", "half_vertical_right",
            "half_horizontal", "half_horizontal_bottom",
            // Squares
            "square_bottom_left", "square_bottom_right",
            "square_top_left", "square_top_right",
            // Borders
            "bordure", "curly_border",
            // Gradients
            "gradient", "gradient_up",
            // Misc
            "bricks", "creeper", "skull", "flow", "globe", "mojang", "piglin",
    };
}
