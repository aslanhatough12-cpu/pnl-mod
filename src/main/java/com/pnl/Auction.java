package com.pnl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Highest single payment wins. Higher bids do NOT reset the timer. First to reach an amount keeps it on ties. */
public final class Auction {
    public static boolean active;
    public static String item = "";
    public static long minBid;
    public static long endAt;
    public static String leader;
    public static long leaderAmount;

    private Auction() {}

    public static void start(String itemName, long min, int seconds) {
        active = true;
        item = itemName.isBlank() ? "Auction" : itemName;
        minBid = min;
        leader = null;
        leaderAmount = 0;
        endAt = System.currentTimeMillis() + seconds * 1000L;
        Hud.toast("Auction started: " + item + " (min " + Money.pretty(min) + ")", 0xFBBF24, true);
    }

    public static void cancel() {
        active = false;
        Hud.toast("Auction cancelled", 0xF87171, false);
    }

    public static long msLeft() {
        return Math.max(0, endAt - System.currentTimeMillis());
    }

    public static void bid(String name, long amount) {
        if (!active || amount < minBid) return;
        if (amount > leaderAmount) {
            leader = name;
            leaderAmount = amount;
        }
    }

    public static void tick(MinecraftClient mc) {
        if (!active || msLeft() > 0) return;
        active = false;
        String msg = leader == null
                ? "Auction over: " + item + " - no valid bids"
                : "Auction over: " + leader + " wins " + item + " with " + Money.pretty(leaderAmount);
        Hud.toast(msg, 0x4ADE80, true);
        if (mc.player != null) mc.player.sendMessage(Text.literal("[PnL] " + msg), false);
    }
}
