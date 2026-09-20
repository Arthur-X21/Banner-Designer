package com.bannerdesigner.client.gui;

import com.bannerdesigner.client.analysis.AnalysisResult;
import com.bannerdesigner.client.analysis.ColorAnalyzer;
import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerLayer;
import com.bannerdesigner.client.banner.BannerRenderer2D;
import com.bannerdesigner.client.design.DesignManager;
import com.bannerdesigner.client.image.ImageLoader;
import com.bannerdesigner.client.image.ImageTexture;
import com.bannerdesigner.client.inventory.InventoryScanner;
import com.bannerdesigner.client.inventory.ResourceReport;
import com.bannerdesigner.client.loom.LoomActionPlan;
import com.bannerdesigner.client.loom.LoomStep;
import com.bannerdesigner.client.preset.PresetEntry;
import com.bannerdesigner.client.preset.PresetManager;
import com.bannerdesigner.client.solver.BannerCandidate;
import com.bannerdesigner.client.solver.SimpleSolver;
import com.bannerdesigner.client.util.BackgroundExecutor;
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
    private static final int COLOR_RED   = 0xFFFF5555;

    private static final int SIDEBAR_X = 10;
    private static final int SIDEBAR_WIDTH = 140;
    private static final int SIDEBAR_TOP = 45;
    private static final int PRESET_BUTTON_HEIGHT = 17;
    private static final int PRESET_BUTTON_SPACING = 2;

    private static final int IMAGE_X = 165;
    private static final int PREVIEW_TOP = 45;
    private static final int PREVIEW_BOTTOM = 150;
    private static final int PANEL_GAP = 8;

    private final LoomScreenHandler loomHandler;
    private ImageTexture currentTexture;
    private ImageTexture bannerTexture;
    private BufferedImage currentImage;
    private List<BannerCandidate> candidates = new ArrayList<>();
    private int selectedCandidate = 0;
    private ResourceReport resourceReport;
    private LoomActionPlan loomPlan;
    private boolean showResources = false;
    private boolean showPlanner = false;
    private boolean busy = false;
    private String loadedPresetName;
    private String busyMessage = "";

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
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(shorten(preset.name(), 18)),
                    b -> onPresetSelected(preset)
            ).dimensions(SIDEBAR_X, y, SIDEBAR_WIDTH, PRESET_BUTTON_HEIGHT).build());
        }

        int btnY = this.height - 122;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.analyze"),
                b -> onAnalyze()
        ).dimensions(SIDEBAR_X, btnY, SIDEBAR_WIDTH, 20).build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.save"),
                b -> onSave()
        ).dimensions(SIDEBAR_X, btnY + 22, SIDEBAR_WIDTH, 20).build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.resources"),
                b -> { this.showResources = !this.showResources; this.showPlanner = false; }
        ).dimensions(SIDEBAR_X, btnY + 44, SIDEBAR_WIDTH, 20).build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.plan"),
                b -> onTogglePlanner()
        ).dimensions(SIDEBAR_X, btnY + 66, SIDEBAR_WIDTH, 20).build());

        int bottomY = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.reload"),
                b -> this.clearAndInit()
        ).dimensions(this.width / 2 - 150, bottomY, 140, 20).build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.back"),
                b -> this.close()
        ).dimensions(this.width / 2 + 10, bottomY, 140, 20).build());
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private int panelWidth() { return (this.width - IMAGE_X - 10 - PANEL_GAP) / 2; }
    private int panelHeight() { return this.height - PREVIEW_TOP - PREVIEW_BOTTOM; }

    private void onPresetSelected(PresetEntry preset) {
        if (this.busy) return;
        destroyTextures();
        this.candidates.clear();
        this.selectedCandidate = 0;
        this.resourceReport = null;
        this.loomPlan = null;
        this.loadedPresetName = preset.name();
        this.busy = true;
        this.busyMessage = "Loading image...";

        BackgroundExecutor.submit(
                () -> ImageLoader.load(preset.path()),
                img -> {
                    this.busy = false;
                    this.busyMessage = "";
                    if (img == null) { sendChat("Failed to load: " + preset.name()); return; }
                    this.currentImage = img;
                    this.currentTexture = ImageTexture.fromBufferedImage(
                            preset.name(), img, panelWidth(), panelHeight());
                    sendChat("Loaded: " + preset.name() + " (" + img.getWidth() + "x" + img.getHeight() + ")");
                },
                err -> {
                    this.busy = false;
                    this.busyMessage = "";
                    sendChat("Load failed: " + err.getMessage());
                }
        );
    }

    private void onAnalyze() {
        if (this.busy) return;
        if (this.currentImage == null) { sendChat("Select a preset first."); return; }

        this.busy = true;
        this.busyMessage = "Analyzing...";

        final BufferedImage target = this.currentImage;
        final int maxCand = com.bannerdesigner.client.config.ConfigManager.maxCandidates();

        BackgroundExecutor.submit(
                () -> {
                    AnalysisResult analysis = ColorAnalyzer.analyze(target);
                    return SimpleSolver.solve(target, analysis, maxCand);
                },
                sol -> {
                    this.busy = false;
                    this.busyMessage = "";
                    this.candidates = sol;
                    this.selectedCandidate = 0;
                    if (sol.isEmpty()) { sendChat("No candidates found."); return; }
                    updateBannerTexture();
                    updateResourceReport();
                    updateLoomPlan();
                    sendChat(String.format("Analyzed. Match: %.1f%% (top of %d)",
                            sol.get(0).score() * 100.0, sol.size()));
                },
                err -> {
                    this.busy = false;
                    this.busyMessage = "";
                    sendChat("Analysis failed: " + err.getMessage());
                }
        );
    }

    private void onSave() {
        if (this.candidates.isEmpty()) { sendChat("Analyze first."); return; }
        BannerCandidate c = this.candidates.get(this.selectedCandidate);
        String name = this.loadedPresetName != null
                ? this.loadedPresetName.replaceAll("\\.[^.]+$", "") : "design";
        DesignManager.save(name, c.definition(), c.score());
        sendChat("Saved design: " + name);
    }

    private void onTogglePlanner() {
        if (this.candidates.isEmpty()) { sendChat("Analyze first."); return; }
        this.showPlanner = !this.showPlanner;
        if (this.showPlanner) this.showResources = false;
    }

    private void updateBannerTexture() {
        if (this.bannerTexture != null) { this.bannerTexture.close(); this.bannerTexture = null; }
        if (this.candidates.isEmpty()) return;
        BannerCandidate c = this.candidates.get(this.selectedCandidate);
        BufferedImage rendered = BannerRenderer2D.render(c.definition());
        this.bannerTexture = ImageTexture.fromBufferedImage("banner_preview", rendered,
                panelWidth(), panelHeight());
    }

    private void updateResourceReport() {
        if (this.candidates.isEmpty()) { this.resourceReport = null; return; }
        BannerCandidate c = this.candidates.get(this.selectedCandidate);
        try { this.resourceReport = InventoryScanner.analyze(c.definition()); }
        catch (Exception e) { this.resourceReport = null; }
    }

    private void updateLoomPlan() {
        if (this.candidates.isEmpty()) { this.loomPlan = null; return; }
        try { this.loomPlan = LoomActionPlan.build(this.candidates.get(this.selectedCandidate).definition()); }
        catch (Exception e) { this.loomPlan = null; }
    }

    private void selectCandidate(int index) {
        if (index < 0 || index >= this.candidates.size()) return;
        this.selectedCandidate = index;
        updateBannerTexture();
        updateResourceReport();
        updateLoomPlan();
    }

    private void sendChat(String msg) {
        if (this.client != null && this.client.player != null)
            this.client.player.sendMessage(Text.literal(msg), false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width/2, 10, COLOR_WHITE);

        List<PresetEntry> presets = PresetManager.snapshot();
        Text info = presets.isEmpty()
                ? Text.translatable("bannerdesigner.info.no_presets")
                : Text.translatable("bannerdesigner.info.presets_found", presets.size());
        context.drawCenteredTextWithShadow(this.textRenderer, info, this.width/2, 24, COLOR_GRAY);

        if (this.busy) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(this.busyMessage), this.width/2, this.height/2, COLOR_YELLOW);
            return;
        }

        int pw = panelWidth(), ph = panelHeight();
        int leftX = IMAGE_X, rightX = IMAGE_X + pw + PANEL_GAP;

        context.drawTextWithShadow(this.textRenderer, Text.literal("Original"), leftX, PREVIEW_TOP-11, COLOR_GRAY);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Banner"), rightX, PREVIEW_TOP-11, COLOR_GRAY);

        if (this.currentTexture != null)
            drawCentered(context, this.currentTexture, leftX, PREVIEW_TOP, pw, ph);

        if (this.bannerTexture != null && !this.candidates.isEmpty()) {
            drawCentered(context, this.bannerTexture, rightX, PREVIEW_TOP, pw, ph);
            BannerCandidate c = this.candidates.get(this.selectedCandidate);
            Text score = Text.literal(String.format("Match: %.1f%%  (#%d/%d)",
                    c.score() * 100.0, this.selectedCandidate + 1, this.candidates.size()));
            context.drawCenteredTextWithShadow(this.textRenderer, score,
                    rightX + pw/2, PREVIEW_TOP + ph + 3, COLOR_GREEN);
            drawLayerInfo(context, rightX, PREVIEW_TOP + ph + 15, pw, c.definition());

            if (this.candidates.size() > 1) {
                int btnY = PREVIEW_TOP + ph + 60;
                int btnW = 28;
                int total = this.candidates.size() * (btnW + 4) - 4;
                int bx = rightX + (pw - total) / 2;
                for (int i = 0; i < this.candidates.size(); i++) {
                    int color = (i == this.selectedCandidate) ? 0xFF55AA55 : 0xFF333333;
                    context.fill(bx, btnY, bx + btnW, btnY + 13, color);
                    context.drawCenteredTextWithShadow(this.textRenderer,
                            Text.literal("#" + (i + 1)), bx + btnW/2, btnY + 3,
                            i == this.selectedCandidate ? COLOR_YELLOW : COLOR_WHITE);
                    bx += btnW + 4;
                }
            }
        }

        if (this.showResources && this.resourceReport != null)
            drawResourcesPanel(context);
        else if (this.showPlanner && this.loomPlan != null)
            drawPlannerPanel(context);
    }

    private void drawLayerInfo(DrawContext context, int x, int y, int w, BannerDefinition def) {
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Base: " + niceName(def.baseColor().name())), x+w/2, y, COLOR_WHITE);
        int ly = y + 11;
        for (int i = 0; i < def.layerCount(); i++) {
            BannerLayer layer = def.layers().get(i);
            String s = (i+1) + ". " + niceName(layer.color().name()) + " " + niceName(layer.patternId());
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(s), x+w/2, ly, COLOR_GRAY);
            ly += 10;
        }
    }

    private void drawResourcesPanel(DrawContext context) {
        int w = 220;
        int x = this.width - w - 10;
        int y = 30, h = this.height - 70;
        context.fill(x, y, x+w, y+h, 0xDD000000);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Resources"), x+6, y+4, COLOR_YELLOW);
        int ly = y + 20;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Required:"), x+6, ly, COLOR_WHITE);
        ly += 11;
        for (ResourceReport.Requirement r : this.resourceReport.required()) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("  " + r.display() + " x" + r.count()), x+6, ly, COLOR_GRAY);
            ly += 10;
        }
        ly += 4;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Missing:"), x+6, ly, COLOR_WHITE);
        ly += 11;
        if (this.resourceReport.missing().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("  All in inventory"), x+6, ly, COLOR_GREEN);
        } else {
            for (ResourceReport.Requirement r : this.resourceReport.missing()) {
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal("  " + r.display() + " x" + r.count()), x+6, ly, COLOR_RED);
                ly += 10;
            }
        }
    }

    private void drawPlannerPanel(DrawContext context) {
        int w = 260;
        int x = this.width - w - 10;
        int y = 30, h = this.height - 70;
        context.fill(x, y, x+w, y+h, 0xDD000000);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Loom Steps (Guided)"), x+6, y+4, COLOR_YELLOW);

        int ly = y + 22;
        int cur = this.loomPlan.currentStep();
        for (int i = 0; i < this.loomPlan.totalSteps(); i++) {
            LoomStep step = this.loomPlan.steps().get(i);
            int col = (i == cur) ? COLOR_YELLOW
                    : (i < cur ? COLOR_GREEN : COLOR_GRAY);
            String prefix = (i == cur) ? "> " : "  ";
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(prefix + step.index() + ". " + step.description()),
                    x+6, ly, col);
            ly += 11;
            if (!step.itemDescription().isEmpty()) {
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal("     " + step.itemDescription()), x+16, ly, COLOR_GRAY);
                ly += 10;
            }
            if (ly > y + h - 12) break;
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
        int texW = tex.width(), texH = tex.height();
        int drawX = areaX + (areaW - texW) / 2;
        int drawY = areaY + (areaH - texH) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, tex.identifier(),
                drawX, drawY, 0f, 0f, texW, texH, texW, texH);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (click.button() == 0 && this.candidates.size() > 1 && !this.busy) {
            int pw = panelWidth(), ph = panelHeight();
            int rightX = IMAGE_X + pw + PANEL_GAP;
            int btnY = PREVIEW_TOP + ph + 60;
            int btnW = 28;
            int total = this.candidates.size() * (btnW + 4) - 4;
            int bx = rightX + (pw - total) / 2;
            for (int i = 0; i < this.candidates.size(); i++) {
                if (click.x() >= bx && click.x() <= bx + btnW
                        && click.y() >= btnY && click.y() <= btnY + 13) {
                    selectCandidate(i); return true;
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
    public void removed() { super.removed(); destroyTextures(); }

    @Override
    public boolean shouldPause() { return false; }
}
