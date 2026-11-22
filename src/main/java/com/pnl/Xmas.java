package com.pnl;

import net.minecraft.client.gui.DrawContext;

/** Christmas look: colors, candy-cane borders, garland and falling snow. */
public final class Xmas {
    public static final int BG = 0xF0102A1E;
    public static final int RED = 0xFFC0392B;
    public static final int GREEN = 0xFF1E8449;
    public static final int GOLD = 0xFFF1C40F;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int DIM = 0xFFB8D4C4;
    public static final int POS = 0xFF4ADE80;
    public static final int NEG = 0xFFF87171;

    private Xmas() {}

    public static void candy(DrawContext c, int x, int y, int w, int h) {
        for (int i = 0; i < w; i += 6) {
            int col = ((i / 6) % 2 == 0) ? RED : WHITE;
            c.fill(x + i, y, Math.min(x + i + 6, x + w), y + h, col);
        }
    }

    /** Big menu panel with candy-cane top and garland bottom. */
    public static void panel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x - 2, y - 2, x + w + 2, y + h + 2, GOLD);
        c.fill(x - 1, y - 1, x + w + 1, y + h + 1, RED);
        c.fill(x, y, x + w, y + h, BG);
        candy(c, x, y, w, 4);
        garland(c, x + 4, y + h - 12, w - 8);
    }

    /** Small box for the on-screen HUD. */
    public static void box(DrawContext c, int x, int y, int w, int h) {
        c.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFFB8860B);
        c.fill(x, y, x + w, y + h, 0xE0102A1E);
        candy(c, x, y, w, 2);
        c.fill(x + 3, y + h - 2, x + 6, y + h, RED);
        c.fill(x + w - 6, y + h - 2, x + w - 3, y + h, GREEN);
    }

    public static void garland(DrawContext c, int x, int y, int w) {
        for (int i = 0; i < w; i += 2) {
            int yy = y + (int) (3 * Math.abs(Math.sin(i * Math.PI / 28.0)));
            c.fill(x + i, yy, x + i + 2, yy + 1, 0xFF2ECC71);
        }
        for (int i = 14; i < w; i += 28) {
            int yy = y + (int) (3 * Math.abs(Math.sin(i * Math.PI / 28.0))) + 1;
            int k = (i / 28) % 3;
            int col = k == 0 ? RED : (k == 1 ? GOLD : 0xFF3B82F6);
            c.fill(x + i - 2, yy, x + i + 2, yy + 5, col);
            c.fill(x + i - 2, yy, x + i + 2, yy + 1, WHITE);
        }
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    public static void snow(DrawContext c, int x, int y, int w, int h, int count) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            double rx = frac(Math.sin(i * 12.9898) * 43758.5453);
            double ry = frac(Math.sin(i * 78.233) * 12345.6789);
            double sp = 0.012 + 0.025 * frac(Math.sin(i * 3.1) * 999.1);
            int px = x + (int) (rx * w + 4 * Math.sin(t / 900.0 + i));
            int py = y + (int) ((ry * h + t * sp) % h);
            px = Math.max(x, Math.min(x + w - 2, px));
            int sz = (i % 3 == 0) ? 2 : 1;
            c.fill(px, py, px + sz, py + sz, 0xCCFFFFFF);
        }
    }
}
