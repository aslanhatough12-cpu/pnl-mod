package com.pnl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One Store per server, saved as config/pnl/<server>.json
 * All stats are calculated from the payment list, so removing a payment fixes every total.
 */
public class Store {
    public static class Payment {
        public int id;
        public String player;
        public long amount;
        public String status = "PENDING"; // PENDING, W (he won, you paid 2x), L (he lost, you kept it)
        public long time;
        public long resolvedAt;
    }

    public static class Row {
        public String name;
        public long wStake, wPayout, lStake;
        public int w, l;
        public long net() { return lStake - wStake; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Store cached;
    private static String cachedKey;

    public int nextId = 1;
    public List<Payment> payments = new ArrayList<>();
    public long pnlResetAt = 0;
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

    // ---------- payments ----------

    public Payment addPayment(String player, long amount) {
        Payment p = new Payment();
        p.id = nextId++;
        p.player = player;
        p.amount = amount;
        p.time = System.currentTimeMillis();
        payments.add(p);
        while (payments.size() > 5000) {
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

    public List<Payment> newestFirst() {
        List<Payment> out = new ArrayList<>(payments);
        Collections.reverse(out);
        return out;
    }

    public void resolveWin(Payment p) {
        p.status = "W";
        p.resolvedAt = System.currentTimeMillis();
        save();
    }

    public void resolveLoss(Payment p) {
        p.status = "L";
        p.resolvedAt = System.currentTimeMillis();
        save();
    }

    public void remove(Payment p) {
        payments.remove(p);
        save();
    }

    public void removePlayer(String name) {
        payments.removeIf(p -> p.player.equals(name));
        save();
    }

    public void clearHistory() {
        payments.clear();
        pnlResetAt = 0;
        save();
    }

    /** Sets the headline profit/loss and win/loss counts back to 0. History stays. */
    public void resetPnl() {
        pnlResetAt = System.currentTimeMillis() + 1;
        save();
    }

    // ---------- stats ----------

    public static long rtime(Payment p) {
        return p.resolvedAt > 0 ? p.resolvedAt : p.time;
    }

    private static boolean resolved(Payment p) {
        return "W".equals(p.status) || "L".equals(p.status);
    }

    /** Profit for you from one payment: L = +amount, W = -amount (you paid 2x, got 1x). */
    public static long net(Payment p) {
        return "L".equals(p.status) ? p.amount : -p.amount;
    }

    private boolean counts(Payment p) {
        return resolved(p) && rtime(p) >= pnlResetAt;
    }

    /** Payments that count toward the headline numbers, oldest first. */
    public List<Payment> counted() {
        List<Payment> out = new ArrayList<>();
        for (Payment p : payments) if (counts(p)) out.add(p);
        out.sort(Comparator.comparingLong(Store::rtime));
        return out;
    }

    public long totalNet() {
        long t = 0;
        for (Payment p : payments) if (counts(p)) t += net(p);
        return t;
    }

    public long todayNet() {
        long t = 0;
        LocalDate today = LocalDate.now();
        ZoneId z = ZoneId.systemDefault();
        for (Payment p : payments) {
            if (counts(p) && Instant.ofEpochMilli(rtime(p)).atZone(z).toLocalDate().equals(today)) t += net(p);
        }
        return t;
    }

    /** Your wins = payments you marked L. */
    public int wins() {
        int n = 0;
        for (Payment p : payments) if (counts(p) && "L".equals(p.status)) n++;
        return n;
    }

    /** Your losses = payments you marked W. */
    public int losses() {
        int n = 0;
        for (Payment p : payments) if (counts(p) && "W".equals(p.status)) n++;
        return n;
    }

    /** Per player totals over the whole history (not affected by the P/L reset). */
    public List<Row> rows() {
        Map<String, Row> m = new LinkedHashMap<>();
        for (Payment p : payments) {
            if (!resolved(p)) continue;
            Row r = m.computeIfAbsent(p.player, k -> {
                Row x = new Row();
                x.name = k;
                return x;
            });
            if ("W".equals(p.status)) {
                r.w++;
                r.wStake += p.amount;
                r.wPayout += p.amount * 2;
            } else {
                r.l++;
                r.lStake += p.amount;
            }
        }
        return new ArrayList<>(m.values());
    }
}
