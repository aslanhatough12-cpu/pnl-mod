package com.pnl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Flat button with a smooth hover fade. */
public class FancyButton extends ButtonWidget {
    private final int color;
    private float hover = 0f;

    public FancyButton(int x, int y, int w, int h, String label, int color, PressAction onPress) {
        super(x, y, w, h, net.minecraft.text.Text.literal(label), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.color = color;
    }

    private static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @Override
    public void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float target = this.isHovered() ? 1f : 0f;
        hover += (target - hover) * Math.min(1f, delta * 0.5f);

        int dark = lerp(0xFF000000, color, 0.55f);
        int c = lerp(dark, color, hover);
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        ctx.fill(x, y, x + w, y + h, lerp(0xFF000000, c, 0.7f));
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, c);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, lerp(c, 0xFFFFFFFF, 0.15f + 0.1f * hover));

        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawCenteredTextWithShadow(mc.textRenderer, getMessage(), x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }
}
