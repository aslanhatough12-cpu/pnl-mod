package com.pnl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Generic "Are you sure?" screen. */
public class SureScreen extends Screen {
    private final String message;
    private final Runnable onYes;
    private final Screen parent;

    public SureScreen(String message, Runnable onYes, Screen parent) {
        super(Text.literal("Are you sure?"));
        this.message = message;
        this.onYes = onYes;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 + 14;
        addDrawableChild(new FancyButton(cx - 100, y, 95, 20, "Yes", Xmas.RED, b -> {
            onYes.run();
            client.setScreen(parent);
        }));
        addDrawableChild(new FancyButton(cx + 5, y, 95, 20, "No", Xmas.GREEN, b -> client.setScreen(parent)));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xAA000000);
        int cx = width / 2, top = height / 2 - 40;
        Xmas.panel(ctx, cx - 140, top, 280, 90);
        Xmas.snow(ctx, cx - 138, top + 4, 276, 80, 20);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Are you sure?"), cx, top + 14, Xmas.GOLD);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(message), cx, top + 30, Xmas.WHITE);
        super.render(ctx, mouseX, mouseY, delta);
    }
}
