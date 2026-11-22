package com.pnl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Top-left boxes, toasts, timer boxes, auction panel and winner popup. All Christmas styled. */
public final class Hud {
    private static class Toast {
        String text;
        int rgb;
        long at;
    }

    private static final List<Toast> toasts = new ArrayList<>();
    private static final long LIFE = 4000;

    private static long sumAt = 0;
    private static long cNet, cToday;
    private static int cWins, cLosses;

    private Hud() {}

    public static void toast(String text, int rgb, boolean sound) {
        Toast t = new Toast();
        t.text = text;
        t.rgb = rgb;
        t.at = System.currentTimeMillis();
        toasts.add(t);
        if (toasts.size() > 5) toasts.remove(0);
        if (sound) Sfx.play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f);
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

    private static void refresh() {
        long now = System.currentTimeMillis();
        if (now - sumAt < 250) return;
        sumAt = now;
        Store st = Store.current();
        cNet = st.totalNet();
        cToday = st.todayNet();
        cWins = st.wins();
        cLosses = st.losses();
    }

    private static void text(DrawContext c, String s, int x, int y, int color) {
        c.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(s), x, y, color);
    }

    private static void center(DrawContext c, String s, int cx, int y, int color) {
        c.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(s), cx, y, color);
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        long now = System.currentTimeMillis();
        Settings se = Settings.get();

        int bx = 6, by = 30;
        int cy = by;

        // profit/loss box + win/loss box
        if (se.showBoxes) {
            refresh();
            Xmas.box(ctx, bx, by, 124, 34);
            text(ctx, Money.signed(cNet), bx + 8, by + 7, cNet >= 0 ? Xmas.POS : Xmas.NEG);
            text(ctx, "Today " + Money.signed(cToday), bx + 8, by + 20, cToday >= 0 ? Xmas.POS : Xmas.NEG);

            int wx = bx + 124 + 4;
            Xmas.box(ctx, wx, by, 60, 34);
            center(ctx, "W", wx + 16, by + 7, Xmas.POS);
            center(ctx, Integer.toString(cWins), wx + 16, by + 20, Xmas.POS);
            center(ctx, "L", wx + 44, by + 7, Xmas.NEG);
            center(ctx, Integer.toString(cLosses), wx + 44, by + 20, Xmas.NEG);
            cy = by + 38;
        }

        // timer + stopwatch boxes
        if (Clock.timerSet) {
            Xmas.box(ctx, bx, cy, 140, 20);
            String label;
            int col = Xmas.GOLD;
            if (Clock.timerDone) {
                label = "Timer DONE!";
                col = ((now / 400) % 2 == 0) ? Xmas.NEG : Xmas.WHITE;
            } else {
                label = "Timer " + Clock.fmt(Clock.timerRemaining(), true) + (Clock.timerRunning ? "" : " (paused)");
            }
            text(ctx, label, bx + 8, cy + 6, col);
            cy += 24;
        }
        if (Clock.swSet) {
            Xmas.box(ctx, bx, cy, 140, 20);
            text(ctx, "Stopwatch " + Clock.fmt(Clock.swElapsed(), false) + (Clock.swRunning ? "" : " (paused)"),
                    bx + 8, cy + 6, Xmas.GOLD);
            cy += 24;
        }

        // toasts (to the right of the boxes)
        toasts.removeIf(t -> now - t.at > LIFE);
        int tx0 = se.showBoxes ? bx + 124 + 4 + 60 + 8 : 8;
        int y = by + (Auction.active ? 28 : 0);
        for (Toast t : toasts) {
            long age = now - t.at;
            float in = Math.min(1f, age / 250f);
            float out = Math.min(1f, (LIFE - age) / 400f);
            int alpha = (int) (Math.min(in, out) * 255);
            if (alpha < 8) continue;
            int w = mc.textRenderer.getWidth(t.text) + 16;
            int x = (int) (tx0 - (1f - in) * (w + 8));
            ctx.fill(x, y, x + w, y + 18, ((alpha * 3 / 4) << 24) | 0x0F2A1D);
            ctx.fill(x, y, x + 2, y + 18, (alpha << 24) | 0xC0392B);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(t.text), x + 8, y + 5, (alpha << 24) | t.rgb);
            y += 22;
        }

        // auction panel
        if (Auction.active) {
            int w = 190, h = 46;
            int x = (mc.getWindow().getScaledWidth() - w) / 2;
            int top = 6;
            Xmas.box(ctx, x, top, w, h);

            long s = (Auction.msLeft() + 999) / 1000;
            String time = String.format("%d:%02d", s / 60, s % 60);
            text(ctx, Auction.item, x + 8, top + 7, Xmas.GOLD);
            text(ctx, time, x + w - 8 - mc.textRenderer.getWidth(time), top + 7, s <= 10 ? Xmas.NEG : Xmas.WHITE);

            if (Auction.leader == null) {
                text(ctx, "No bids yet (min " + Money.pretty(Auction.minBid) + ")", x + 8, top + 28, Xmas.DIM);
            } else {
                drawHead(ctx, Auction.leader, x + 8, top + 22, 18);
                text(ctx, Auction.leader, x + 32, top + 24, Xmas.WHITE);
                text(ctx, Money.pretty(Auction.leaderAmount), x + 32, top + 34, Xmas.POS);
            }
        }

        // winner popup
        if (now < Auction.winnerUntil) {
            int w = 240, h = 86;
            int x = (mc.getWindow().getScaledWidth() - w) / 2;
            int top = 70;
            Xmas.box(ctx, x, top, w, h);
            Xmas.candy(ctx, x, top + 2, w, 2);
            center(ctx, "* AUCTION WINNER *", x + w / 2, top + 10, Xmas.GOLD);
            center(ctx, Auction.winnerItem, x + w / 2, top + 24, Xmas.DIM);
            if (Auction.winnerName == null) {
                center(ctx, "No valid bids", x + w / 2, top + 48, Xmas.WHITE);
            } else {
                drawHead(ctx, Auction.winnerName, x + 24, top + 40, 32);
                text(ctx, Auction.winnerName, x + 66, top + 46, Xmas.WHITE);
                text(ctx, Money.pretty(Auction.winnerAmount), x + 66, top + 60, Xmas.POS);
            }
            Xmas.snow(ctx, x + 2, top + 4, w - 4, h - 8, 18);
        }
    }
}
