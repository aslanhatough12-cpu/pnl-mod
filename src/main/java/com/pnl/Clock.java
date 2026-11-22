package com.pnl;

import net.minecraft.client.MinecraftClient;

/** One countdown timer (max 24h) and one stopwatch. */
public final class Clock {
    // timer
    public static boolean timerSet, timerRunning, timerDone;
    private static long timerEndAt, timerLeft;

    // stopwatch
    public static boolean swSet, swRunning;
    private static long swStart, swAcc;

    private Clock() {}

    public static void startTimer(long ms) {
        timerSet = true;
        timerDone = false;
        timerRunning = true;
        timerLeft = ms;
        timerEndAt = System.currentTimeMillis() + ms;
    }

    /** Pauses if running, resumes if paused. */
    public static void pauseTimer() {
        if (!timerSet || timerDone) return;
        long now = System.currentTimeMillis();
        if (timerRunning) {
            timerLeft = Math.max(0, timerEndAt - now);
            timerRunning = false;
        } else if (timerLeft > 0) {
            timerEndAt = now + timerLeft;
            timerRunning = true;
        }
    }

    public static void resetTimer() {
        timerSet = false;
        timerRunning = false;
        timerDone = false;
    }

    public static long timerRemaining() {
        return timerRunning ? Math.max(0, timerEndAt - System.currentTimeMillis()) : timerLeft;
    }

    public static void startSw() {
        if (swRunning) return;
        if (!swSet) {
            swSet = true;
            swAcc = 0;
        }
        swStart = System.currentTimeMillis();
        swRunning = true;
    }

    public static void pauseSw() {
        if (!swSet) return;
        if (swRunning) {
            swAcc += System.currentTimeMillis() - swStart;
            swRunning = false;
        } else {
            swStart = System.currentTimeMillis();
            swRunning = true;
        }
    }

    public static void resetSw() {
        swSet = false;
        swRunning = false;
        swAcc = 0;
    }

    public static long swElapsed() {
        return swAcc + (swRunning ? System.currentTimeMillis() - swStart : 0);
    }

    public static void tick(MinecraftClient mc) {
        if (timerRunning && System.currentTimeMillis() >= timerEndAt) {
            timerRunning = false;
            timerLeft = 0;
            timerDone = true;
            Hud.toast("Timer finished!", 0xFFD700, false);
            Sfx.jingle();
        }
    }

    public static String fmt(long ms, boolean roundUp) {
        long s = roundUp ? (ms + 999) / 1000 : ms / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, sec);
        return String.format("%d:%02d", m, sec);
    }
}
