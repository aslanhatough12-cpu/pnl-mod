package com.pnl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One Store per server, saved as config/pnl/<server>.json */
public class Store {
    public static class Payment {
        public int id;
        public String player;
        public long amount;
        public String status = "PENDING"; // PENDING, W, L
        public long time;
    }

    public static class Stat {
        public long lStake;   // total they paid me on bets they LOST
        public long wStake;   // total they paid me on bets they WON
        public long wPayout;  // total I paid them out (2x)
        public int w, l;
        public long net() { return lStake - wStake; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Store cached;
    private static String cachedKey;

    public int nextId = 1;
    public List<Payment> payments = new ArrayList<>();
    public Map<String, Stat> stats = new HashMap<>();
    /** false = pay command uses 4.8M style, true = full number 4800000 */
    public boolean fullNumberPay = false;
    /** payouts above this need a second confirm screen */
    public long twoStepOver = 100_000_000L;

    private transient String key = "singleplayer";

    public static String serverKey() {
        ServerInfo si = MinecraftClient.getInstance().getCurrentServerEntry();
        if (si == null) return "singleplayer";
        return si.address.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    public static Store current() {
        String k = serverKey();
        if (cached == null || !k.equals(cachedKey)) {
            cached = load(k);
            cachedKey = k;
        }
        return cached;
    }

    private static Path file(String k) {
        return FabricLoader.getInstance().getConfigDir().resolve("pnl").resolve(k + ".json");
    }

    private static Store load(String k) {
        Store s = null;
        try {
            Path f = file(k);
            if (Files.exists(f)) s = GSON.fromJson(Files.readString(f), Store.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (s == null) s = new Store();
        if (s.payments == null) s.payments = new ArrayList<>();
        if (s.stats == null) s.stats = new HashMap<>();
        s.key = k;
        return s;
    }

    public void save() {
        try {
            Path f = file(key);
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Payment addPayment(String player, long amount) {
        Payment p = new Payment();
        p.id = nextId++;
        p.player = player;
        p.amount = amount;
        p.time = System.currentTimeMillis();
        payments.add(p);
        while (payments.size() > 300) {
            // drop the oldest already-handled entry
            boolean removed = false;
            for (int i = 0; i < payments.size(); i++) {
                if (!payments.get(i).status.equals("PENDING")) { payments.remove(i); removed = true; break; }
            }
            if (!removed) payments.remove(0);
        }
        save();
        return p;
    }

    public Payment find(int id) {
        for (Payment p : payments) if (p.id == id) return p;
        return null;
    }

    /** Newest first. */
    public List<Payment> pending(int max) {
        List<Payment> out = new ArrayList<>();
        for (int i = payments.size() - 1; i >= 0 && out.size() < max; i--) {
            if (payments.get(i).status.equals("PENDING")) out.add(payments.get(i));
        }
        return out;
    }

    public void resolveWin(Payment p) {
        p.status = "W";
        Stat s = stats.computeIfAbsent(p.player, k -> new Stat());
        s.w++;
        s.wStake += p.amount;
        s.wPayout += p.amount * 2;
        save();
    }

    public void resolveLoss(Payment p) {
        p.status = "L";
        Stat s = stats.computeIfAbsent(p.player, k -> new Stat());
        s.l++;
        s.lStake += p.amount;
        save();
    }

    public long totalNet() {
        long t = 0;
        for (Stat s : stats.values()) t += s.net();
        return t;
    }
}
