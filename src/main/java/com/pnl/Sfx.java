package com.pnl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** All mod sounds go through here so the volume setting works. */
public final class Sfx {
    private static class Queued {
        SoundEvent ev;
        float vol, pitch;
        long at;
    }

    private static final List<Queued> queue = new ArrayList<>();

    private Sfx() {}

    public static void play(SoundEvent e, float vol, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        float v = vol * Settings.get().volume / 100f;
        if (v <= 0f) return;
        mc.player.playSound(e, v, pitch);
    }

    public static void play(RegistryEntry<SoundEvent> e, float vol, float pitch) {
        play(e.value(), vol, pitch);
    }

    public static void playLater(SoundEvent e, float vol, float pitch, long delayMs) {
        Queued q = new Queued();
        q.ev = e;
        q.vol = vol;
        q.pitch = pitch;
        q.at = System.currentTimeMillis() + delayMs;
        queue.add(q);
    }

    public static void playLater(RegistryEntry<SoundEvent> e, float vol, float pitch, long delayMs) {
        playLater(e.value(), vol, pitch, delayMs);
    }

    public static void tick() {
        if (queue.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<Queued> it = queue.iterator();
        List<Queued> due = new ArrayList<>();
        while (it.hasNext()) {
            Queued q = it.next();
            if (q.at <= now) {
                due.add(q);
                it.remove();
            }
        }
        for (Queued q : due) play(q.ev, q.vol, q.pitch);
    }

    /** Little jingle-bells style tune. */
    public static void jingle() {
        float[] p = {0.89f, 0.89f, 0.89f, 0.89f, 0.89f, 0.89f, 0.89f, 1.06f, 0.71f, 0.79f, 0.89f};
        long[] d = {0, 180, 360, 640, 820, 1000, 1280, 1460, 1640, 1820, 2000};
        for (int i = 0; i < p.length; i++) {
            playLater(SoundEvents.BLOCK_NOTE_BLOCK_BELL, 0.9f, p[i], d[i]);
        }
    }
}
