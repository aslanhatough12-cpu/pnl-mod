package com.pnl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Top-left toasts + the auction panel at the top of the screen. */
public final class Hud {
    private static class Toast {
        String text;
        int rgb;
        long at;
    }

    private static final List<Toast> toasts = new ArrayList<>();
    private static final long LIFE = 4000;

    private Hud() {}

    public static void toast(String text, int rgb, boolean sound) {
        Toast t = new Toast();
        t.text = text;
        t.rgb = rgb;
        t.at = System.currentTimeMillis();
        toasts.add(t);
        if (toasts.size() > 5) toasts.remove(0);
        if (sound) {
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f));
        }
    }

    public static void toast(String text, int rgb) {
        toast(text, rgb, false);
    }

    public static void drawHead(DrawContext ctx, String name, int x, int y, int size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        PlayerListEntry e = nh == null ? null : nh.getPlayerListEntry(name);
        if (e == null) {
            ctx.fill(x, y, x + size, y + size, 0xFF4B5563);
            return;
        }
        PlayerSkinDrawer.draw(ctx, e.getSkinTextures(), x, y, size);
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        long now = System.currentTimeMillis();

        toasts.removeIf(t -> now - t.at > LIFE);
        int y = 8;
        for (Toast t : toasts) {
            long age = now - t.at;
            float in = Math.min(1f, age / 250f);
            float out = Math.min(1f, (LIFE - age) / 400f);
            int alpha = (int) (Math.min(in, out) * 255);
            if (alpha < 8) continue;
            int w = mc.textRenderer.getWidth(t.text) + 16;
            int x = (int) (8 - (1f - in) * (w + 8));
            ctx.fill(x, y, x + w, y + 18, ((alpha * 3 / 4) << 24) | 0x101418);
            ctx.fill(x, y, x + 2, y + 18, (alpha << 24) | t.rgb);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(t.text), x + 8, y + 5, (alpha << 24) | t.rgb);
            y += 22;
        }

        if (Auction.active) {
            int w = 190, h = 46;
            int x = (mc.getWindow().getScaledWidth() - w) / 2;
            int top = 6;
            ctx.fill(x - 1, top - 1, x + w + 1, top + h + 1, 0xFF2A3140);
            ctx.fill(x, top, x + w, top + h, 0xE6141821);

            long s = (Auction.msLeft() + 999) / 1000;
            String time = String.format("%d:%02d", s / 60, s % 60);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(Auction.item), x + 8, top + 6, 0xFFFBBF24);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(time), x + w - 8 - mc.textRenderer.getWidth(time), top + 6, 0xFFFFFFFF);

            if (Auction.leader == null) {
                ctx.drawTextWithShadow(mc.textRenderer, Text.literal("No bids yet (min " + Money.pretty(Auction.minBid) + ")"), x + 8, top + 28, 0xFF9CA3AF);
            } else {
                drawHead(ctx, Auction.leader, x + 8, top + 22, 18);
                ctx.drawTextWithShadow(mc.textRenderer, Text.literal(Auction.leader), x + 32, top + 24, 0xFFFFFFFF);
                ctx.drawTextWithShadow(mc.textRenderer, Text.literal(Money.pretty(Auction.leaderAmount)), x + 32, top + 34, 0xFF4ADE80);
            }
        }
    }
}
