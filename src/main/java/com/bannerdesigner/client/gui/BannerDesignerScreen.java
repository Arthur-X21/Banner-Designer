package com.bannerdesigner.client.gui;

import com.bannerdesigner.client.analysis.AnalysisResult;
import com.bannerdesigner.client.analysis.ColorAnalyzer;
import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import com.bannerdesigner.client.banner.BannerRenderer2D;
import com.bannerdesigner.client.cache.CacheManager;
import com.bannerdesigner.client.image.ImageLoader;
import com.bannerdesigner.client.image.ImageTexture;
import com.bannerdesigner.client.preset.PresetEntry;
import com.bannerdesigner.client.preset.PresetManager;
import com.bannerdesigner.client.solver.BannerCandidate;
import com.bannerdesigner.client.solver.SimpleSolver;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.text.Text;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BannerDesignerScreen extends Screen {

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY  = 0xFFAAAAAA;
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_YELLOW = 0xFFFFFF55;

    private static final int SIDEBAR_X = 10;
    private static final int SIDEBAR_WIDTH = 150;
    private static final int SIDEBAR_TOP = 45;
    private static final int PRESET_BUTTON_HEIGHT = 18;
    private static final int PRESET_BUTTON_SPACING = 3;

    private static final int IMAGE_X = 175;
    private static final int PREVIEW_TOP = 45;
    private static final int PREVIEW_BOTTOM = 90;
    private static final int PANEL_GAP = 10;

    private final LoomScreenHandler loomHandler;
    private ImageTexture currentTexture;
    private ImageTexture bannerTexture;
    private BufferedImage currentImage;
    private byte[] currentImageBytes;
    private List<BannerCandidate> candidates = new ArrayList<>();
    private int selectedCandidate = 0;
    private String statusMessage;

    public BannerDesignerScreen(LoomScreenHandler loomHandler) {
        super(Text.translatable("bannerdesigner.screen.title"));
        this.loomHandler = loomHandler;
    }

    @Override
    protected void init() {
        super.init();

        PresetManager.reload();
        List<PresetEntry> presets = PresetManager.snapshot();

        int availableHeight = this.height - SIDEBAR_TOP - 130;
        int perButton = PRESET_BUTTON_HEIGHT + PRESET_BUTTON_SPACING;
        int maxVisible = Math.max(1, availableHeight / perButton);
        int visibleCount = Math.min(presets.size(), maxVisible);

        for (int i = 0; i < visibleCount; i++) {
            PresetEntry preset = presets.get(i);
            int y = SIDEBAR_TOP + i * perButton;

            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(preset.name()),
                    b -> onPresetSelected(preset)
            ).dimensions(SIDEBAR_X, y, SIDEBAR_WIDTH, PRESET_BUTTON_HEIGHT).build();

            this.addDrawableChild(button);
        }

        int analyzeY = this.height - 92;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.analyze"),
                b -> onAnalyze()
        ).dimensions(SIDEBAR_X, analyzeY, SIDEBAR_WIDTH, 20).build());

        int bottomY = this.height - 30;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.reload"),
                b -> this.clearAndInit()
        ).dimensions(this.width / 2 - 150, bottomY, 140, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.back"),
                b -> this.close()
        ).dimensions(this.width / 2 + 10, bottomY, 140, 20).build());
    }

    private int panelWidth() {
        int availableW = this.width - IMAGE_X - 10;
        return (availableW - PANEL_GAP) / 2;
    }

    private int panelHeight() {
        return this.height - PREVIEW_TOP - PREVIEW_BOTTOM;
    }

    private void onPresetSelected(PresetEntry preset) {
        destroyTextures();
        this.candidates.clear();
        this.selectedCandidate = 0;

        BufferedImage img = ImageLoader.load(preset.path());
        if (img == null) {
            this.statusMessage = "Failed to load: " + preset.name();
        } else {
            this.currentImage = img;
            this.currentImageBytes = CacheManager.hashBytes(preset.name().getBytes()).getBytes();
            int pw = panelWidth();
            int ph = panelHeight();
            this.currentTexture = ImageTexture.fromBufferedImage(preset.name(), img, pw, ph);
            this.statusMessage = "Loaded: " + preset.name()
                    + " (" + img.getWidth() + "x" + img.getHeight() + ")";
        }

        sendChat(this.statusMessage);
    }

    private void onAnalyze() {
        if (this.currentImage == null) {
            sendChat("Select a preset first.");
            return;
        }

        try {
            AnalysisResult analysis = ColorAnalyzer.analyze(this.currentImage);
            this.candidates = SimpleSolver.solve(
                    this.currentImage, analysis,
                    com.bannerdesigner.client.config.ConfigManager.maxCandidates());
            this.selectedCandidate = 0;

            if (this.candidates.isEmpty()) {
                sendChat("No candidates found.");
                return;
            }

            updateBannerTexture();
            BannerCandidate best = this.candidates.get(0);
            sendChat(String.format("Analyzed. Match: %.1f%% (top of %d)",
                    best.score() * 100.0, this.candidates.size()));
        } catch (Exception e) {
            sendChat("Analysis failed: " + e.getMessage());
        }
    }

    private void updateBannerTexture() {
        if (this.bannerTexture != null) {
            this.bannerTexture.close();
            this.bannerTexture = null;
        }
        if (this.candidates.isEmpty()) return;

        BannerCandidate c = this.candidates.get(this.selectedCandidate);
        BufferedImage rendered = BannerRenderer2D.render(c.definition());
        int pw = panelWidth();
        int ph = panelHeight();
        this.bannerTexture = ImageTexture.fromBufferedImage("banner_preview", rendered, pw, ph);
    }

    private void selectCandidate(int index) {
        if (index < 0 || index >= this.candidates.size()) return;
        this.selectedCandidate = index;
        updateBannerTexture();
    }

    private void sendChat(String msg) {
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal(msg), false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 12, COLOR_WHITE
        );

        List<PresetEntry> presets = PresetManager.snapshot();
        Text info = presets.isEmpty()
                ? Text.translatable("bannerdesigner.info.no_presets")
                : Text.translatable("bannerdesigner.info.presets_found", presets.size());
        context.drawCenteredTextWithShadow(this.textRenderer, info, this.width / 2, 26, COLOR_GRAY);

        int pw = panelWidth();
        int ph = panelHeight();
        int leftX = IMAGE_X;
        int rightX = IMAGE_X + pw + PANEL_GAP;

        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Original"), leftX, PREVIEW_TOP - 12, COLOR_GRAY);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Banner"), rightX, PREVIEW_TOP - 12, COLOR_GRAY);

        if (this.currentTexture != null) {
            drawCentered(context, this.currentTexture, leftX, PREVIEW_TOP, pw, ph);
        }

        if (this.bannerTexture != null && !this.candidates.isEmpty()) {
            drawCentered(context, this.bannerTexture, rightX, PREVIEW_TOP, pw, ph);

            BannerCandidate c = this.candidates.get(this.selectedCandidate);
            Text score = Text.literal(String.format("Match: %.1f%%  (#%d/%d)",
                    c.score() * 100.0, this.selectedCandidate + 1, this.candidates.size()));
            context.drawCenteredTextWithShadow(this.textRenderer, score,
                    rightX + pw / 2, PREVIEW_TOP + ph + 4, COLOR_GREEN);

            drawLayerInfo(context, rightX, PREVIEW_TOP + ph + 18, pw, c.definition());
        }

        // Candidate selector buttons at bottom of preview area
        if (this.candidates.size() > 1) {
            int btnY = PREVIEW_TOP + ph + 40;
            int btnW = 30;
            int total = this.candidates.size() * (btnW + 4) - 4;
            int bx = rightX + (pw - total) / 2;
            for (int i = 0; i < this.candidates.size(); i++) {
                final int idx = i;
                String label = "#" + (i + 1);
                context.fill(bx, btnY, bx + btnW, btnY + 14,
                        i == this.selectedCandidate ? 0xFF55AA55 : 0xFF333333);
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal(label), bx + btnW / 2, btnY + 3,
                        i == this.selectedCandidate ? COLOR_YELLOW : COLOR_WHITE);
                bx += btnW + 4;
            }
        }
    }

    private void drawLayerInfo(DrawContext context, int x, int y, int w, BannerDefinition def) {
        String baseLine = "Base: " + niceName(def.baseColor().name().toLowerCase());
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(baseLine), x + w / 2, y, COLOR_GRAY);

        int ly = y + 11;
        int max = Math.min(def.layerCount(), 4);
        for (int i = 0; i < max; i++) {
            BannerLayer layer = def.layers().get(i);
            String s = (i + 1) + ". " + niceName(layer.color().name().toLowerCase())
                    + " " + niceName(layer.patternId());
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(s), x + w / 2, ly, COLOR_GRAY);
            ly += 10;
        }
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

    private void drawCentered(DrawContext context, ImageTexture tex,
                               int areaX, int areaY, int areaW, int areaH) {
        int texW = tex.width();
        int texH = tex.height();
        int drawX = areaX + (areaW - texW) / 2;
        int drawY = areaY + (areaH - texH) / 2;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                tex.identifier(),
                drawX, drawY,
                0.0f, 0.0f,
                texW, texH,
                texW, texH
        );
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (click.button() == 0 && this.candidates.size() > 1) {
            int pw = panelWidth();
            int ph = panelHeight();
            int rightX = IMAGE_X + pw + PANEL_GAP;
            int btnY = PREVIEW_TOP + ph + 40;
            int btnW = 30;
            int total = this.candidates.size() * (btnW + 4) - 4;
            int bx = rightX + (pw - total) / 2;

            for (int i = 0; i < this.candidates.size(); i++) {
                if (click.x() >= bx && click.x() <= bx + btnW
                        && click.y() >= btnY && click.y() <= btnY + 14) {
                    selectCandidate(i);
                    return true;
                }
                bx += btnW + 4;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void destroyTextures() {
        if (this.currentTexture != null) { this.currentTexture.close(); this.currentTexture = null; }
        if (this.bannerTexture != null) { this.bannerTexture.close(); this.bannerTexture = null; }
    }

    @Override
    public void removed() {
        super.removed();
        destroyTextures();
    }

    @Override
    public boolean shouldPause() { return false; }
}
