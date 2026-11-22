package com.pnl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Highest single payment wins. Higher bids do NOT reset the timer. First to reach an amount keeps it on ties. */
public final class Auction {
    public static class Bid {
        public String name;
        public long amount;
    }

    public static boolean active;
    public static String item = "";
    public static long minBid;
    public static long endAt;
    public static String leader;
    public static long leaderAmount;
    public static final List<Bid> bids = new ArrayList<>();

    // winner popup
    public static long winnerUntil;
    public static String winnerName;
    public static long winnerAmount;
    public static String winnerItem = "";

    private static long lastBeepSec = -1;

    private Auction() {}

    public static void start(String itemName, long min, int seconds) {
        active = true;
        item = itemName.isBlank() ? "Auction" : itemName;
        minBid = min;
        leader = null;
        leaderAmount = 0;
        bids.clear();
        winnerUntil = 0;
        lastBeepSec = -1;
        endAt = System.currentTimeMillis() + seconds * 1000L;
        Hud.toast("Auction started: " + item + " (min " + Money.pretty(min) + ")", 0xFFD700, true);
    }

    public static void cancel() {
        active = false;
        winnerUntil = 0;
        Hud.toast("Auction cancelled", 0xF87171, false);
    }

    public static long msLeft() {
        return Math.max(0, endAt - System.currentTimeMillis());
    }

    public static void bid(String name, long amount) {
        if (!active || amount < minBid) return;
        Bid b = new Bid();
        b.name = name;
        b.amount = amount;
        bids.add(b);
        if (amount > leaderAmount) {
            leader = name;
            leaderAmount = amount;
        }
    }

    public static void tick(MinecraftClient mc) {
        if (!active) return;
        long ms = msLeft();
        long s = (ms + 999) / 1000;
        if (ms > 0 && s <= 10 && s != lastBeepSec) {
            lastBeepSec = s;
            Sfx.play(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.9f, s <= 3 ? 1.8f : 1.3f);
        }
        if (ms > 0) return;

        active = false;
        winnerName = leader;
        winnerAmount = leaderAmount;
        winnerItem = item;
        winnerUntil = System.currentTimeMillis() + 9000;
        Sfx.jingle();
        String msg = leader == null
                ? "Auction over: " + item + " - no valid bids"
                : "Auction over: " + leader + " wins " + item + " with " + Money.pretty(leaderAmount);
        Hud.toast(msg, 0x4ADE80, false);
        if (mc.player != null) mc.player.sendMessage(Text.literal("[PnL] " + msg), false);
    }
}
