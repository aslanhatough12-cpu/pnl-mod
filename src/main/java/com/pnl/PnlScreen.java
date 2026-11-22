package com.pnl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The K menu. Tabs: Gambles, History, Auction, Timer, Calc, Graph, Setup. */
public class PnlScreen extends Screen {
    private static final String[] TABS = {"Gambles", "History", "Auction", "Timer", "Calc", "Graph", "Setup"};
    private static final int ROWS_G = 8, ROWS_H = 11, ROWS_B = 6;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private static String lastItem = "", lastMin = "1M", lastSecs = "60", lastExpr = "";
    private static String lastH = "0", lastM = "5", lastS = "0";
    private static int sort = 0; // 0 most profit, 1 most lost, 2 most wins

    private int tab = 0, scroll = 0;
    private TextFieldWidget fItem, fMin, fSecs, fH, fM, fS, fExpr;
    private final int pw = 420, ph = 272;
    private int px, py;

    public PnlScreen() {
        super(Text.literal("PnL"));
    }

    // ---------------- helpers ----------------

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(s), x, y, color);
    }

    private TextFieldWidget field(int x, int y, int w, int max, String init) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 18, Text.literal(""));
        f.setMaxLength(max);
        f.setText(init);
        addDrawableChild(f);
        return f;
    }

    private void saveFields() {
        if (fItem != null) lastItem = fItem.getText();
        if (fMin != null) lastMin = fMin.getText();
        if (fSecs != null) lastSecs = fSecs.getText();
        if (fH != null) lastH = fH.getText();
        if (fM != null) lastM = fM.getText();
        if (fS != null) lastS = fS.getText();
        if (fExpr != null) lastExpr = fExpr.getText();
    }

    private void rebuild() {
        saveFields();
        clearAndInit();
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Store.Row> sortedRows(Store st) {
        List<Store.Row> rows = st.rows();
        if (sort == 0) rows.sort((a, b) -> Long.compare(b.net(), a.net()));
        else if (sort == 1) rows.sort((a, b) -> Long.compare(a.net(), b.net()));
        else rows.sort((a, b) -> Integer.compare(b.l, a.l));
        return rows;
    }

    // ---------------- init ----------------

    @Override
    protected void init() {
        fItem = fMin = fSecs = fH = fM = fS = fExpr = null;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        Store st = Store.current();
        Settings se = Settings.get();

        for (int i = 0; i < TABS.length; i++) {
            final int n = i;
            addDrawableChild(new FancyButton(px + 8 + i * 58, py + 10, 56, 16, TABS[i],
                    tab == i ? Xmas.RED : 0xFF1E5A3C, b -> { saveFields(); tab = n; scroll = 0; clearAndInit(); }));
        }

        switch (tab) {
            case 0: initGambles(st, se); break;
            case 1: initHistory(st); break;
            case 2: initAuction(); break;
            case 3: initTimer(); break;
            case 4: fExpr = field(px + 10, py + 44, pw - 20, 80, lastExpr); break;
            case 5: break;
            default: initSetup(se); break;
        }
    }

    private void initGambles(Store st, Settings se) {
        addDrawableChild(new FancyButton(px + 10, py + 44, 76, 16, se.showBoxes ? "Boxes: ON" : "Boxes: OFF",
                se.showBoxes ? Xmas.GREEN : 0xFF6B7280, b -> { se.showBoxes = !se.showBoxes; se.save(); rebuild(); }));

        String[] sl = {"Most profit", "Most lost", "Most wins"};
        for (int i = 0; i < 3; i++) {
            final int n = i;
            addDrawableChild(new FancyButton(px + 94 + i * 66, py + 44, 64, 16, sl[i],
                    sort == i ? Xmas.RED : 0xFF1E5A3C, b -> { sort = n; scroll = 0; rebuild(); }));
        }

        if (!Auction.active) {
            List<Store.Payment> pend = st.pending(3);
            for (int i = 0; i < pend.size(); i++) {
                Store.Payment p = pend.get(i);
                int y = py + 80 + i * 16;
                addDrawableChild(new FancyButton(px + pw - 72, y, 30, 14, "W", Xmas.GREEN,
                        b -> client.setScreen(new ConfirmPayScreen(p, this))));
                addDrawableChild(new FancyButton(px + pw - 38, y, 30, 14, "L", Xmas.RED, b -> {
                    st.resolveLoss(p);
                    Hud.toast("L  " + p.player + " lost " + Money.pretty(p.amount), 0xEF4444, false);
                    rebuild();
                }));
            }
        }

        List<Store.Row> rows = sortedRows(st);
        for (int i = 0; i < ROWS_G; i++) {
            int idx = i + scroll;
            if (idx >= rows.size()) break;
            String name = rows.get(idx).name;
            int y = py + 152 + i * 12;
            addDrawableChild(new FancyButton(px + pw - 20, y, 10, 10, "x", Xmas.RED,
                    b -> client.setScreen(new SureScreen("Remove all of " + name + "'s payments?",
                            () -> st.removePlayer(name), this))));
        }
    }

    private void initHistory(Store st) {
        List<Store.Payment> all = st.newestFirst();
        for (int i = 0; i < ROWS_H; i++) {
            int idx = i + scroll;
            if (idx >= all.size()) break;
            Store.Payment p = all.get(idx);
            int y = py + 48 + i * 14;
            addDrawableChild(new FancyButton(px + pw - 22, y - 1, 12, 11, "x", Xmas.RED,
                    b -> client.setScreen(new SureScreen("Remove this payment from " + p.player + "?",
                            () -> st.remove(p), this))));
        }
        addDrawableChild(new FancyButton(px + 10, py + ph - 32, 96, 16, "Reset P/L", 0xFFB45309,
                b -> client.setScreen(new SureScreen("Set profit/loss back to 0? (history stays)",
                        () -> st.resetPnl(), this))));
        addDrawableChild(new FancyButton(px + 112, py + ph - 32, 110, 16, "Clear history", Xmas.RED,
                b -> client.setScreen(new SureScreen("Remove EVERY payment and all stats?",
                        () -> st.clearHistory(), this))));
    }

    private void initAuction() {
        fItem = field(px + 100, py + 40, 220, 40, lastItem);
        fMin = field(px + 100, py + 64, 120, 16, lastMin);
        fSecs = field(px + 100, py + 88, 60, 3, lastSecs);
        addDrawableChild(new FancyButton(px + 100, py + 114, 110, 20, "Start auction", Xmas.GREEN, b -> startAuction()));
        if (Auction.active) {
            addDrawableChild(new FancyButton(px + 216, py + 114, 104, 20, "Cancel auction", Xmas.RED, b -> {
                Auction.cancel();
                rebuild();
            }));
        }
    }

    private void startAuction() {
        long min = Money.parse(fMin.getText());
        int secs;
        try {
            secs = Integer.parseInt(fSecs.getText().trim());
        } catch (Exception e) {
            secs = -1;
        }
        if (min <= 0 || secs < 5 || secs > 120) {
            Hud.toast("Check min bid (like 5M) and seconds (5-120)", 0xF87171, false);
            return;
        }
        saveFields();
        Auction.start(fItem.getText().trim(), min, secs);
        client.setScreen(null);
    }

    private void initTimer() {
        fH = field(px + 100, py + 48, 40, 3, lastH);
        fM = field(px + 150, py + 48, 40, 3, lastM);
        fS = field(px + 200, py + 48, 40, 3, lastS);

        addDrawableChild(new FancyButton(px + 10, py + 76, 70, 16, "Start", Xmas.GREEN, b -> startTimer()));
        addDrawableChild(new FancyButton(px + 84, py + 76, 70, 16, Clock.timerRunning ? "Pause" : "Resume", 0xFFB45309,
                b -> { Clock.pauseTimer(); rebuild(); }));
        addDrawableChild(new FancyButton(px + 158, py + 76, 70, 16, "Reset", Xmas.RED,
                b -> { Clock.resetTimer(); rebuild(); }));

        addDrawableChild(new FancyButton(px + 10, py + 146, 70, 16, "Start", Xmas.GREEN,
                b -> { Clock.startSw(); rebuild(); }));
        addDrawableChild(new FancyButton(px + 84, py + 146, 70, 16, Clock.swRunning ? "Pause" : "Resume", 0xFFB45309,
                b -> { Clock.pauseSw(); rebuild(); }));
        addDrawableChild(new FancyButton(px + 158, py + 146, 70, 16, "Reset", Xmas.RED,
                b -> { Clock.resetSw(); rebuild(); }));
    }

    private void startTimer() {
        long total = num(fH.getText()) * 3600 + num(fM.getText()) * 60 + num(fS.getText());
        if (total <= 0 || total > 24 * 3600) {
            Hud.toast("Timer must be between 1 second and 24 hours", 0xF87171, false);
            return;
        }
        saveFields();
        Clock.startTimer(total * 1000L);
        rebuild();
    }

    private void initSetup(Settings se) {
        addDrawableChild(new FancyButton(px + 120, py + 44, 24, 16, "-", Xmas.RED, b -> {
            se.volume = Math.max(0, se.volume - 10);
            se.save();
            rebuild();
        }));
        addDrawableChild(new FancyButton(px + 200, py + 44, 24, 16, "+", Xmas.GREEN, b -> {
            se.volume = Math.min(100, se.volume + 10);
            se.save();
            rebuild();
        }));
        addDrawableChild(new FancyButton(px + 10, py + 74, 100, 16, "Test sound", 0xFFB45309, b -> Sfx.jingle()));
        addDrawableChild(new FancyButton(px + 10, py + 108, 160, 16, se.dmAlerts ? "Message alerts: ON" : "Message alerts: OFF",
                se.dmAlerts ? Xmas.GREEN : 0xFF6B7280, b -> { se.dmAlerts = !se.dmAlerts; se.save(); rebuild(); }));
        addDrawableChild(new FancyButton(px + 10, py + 130, 160, 16, se.mentionAlerts ? "Mention alerts: ON" : "Mention alerts: OFF",
                se.mentionAlerts ? Xmas.GREEN : 0xFF6B7280, b -> { se.mentionAlerts = !se.mentionAlerts; se.save(); rebuild(); }));
    }

    // ---------------- input ----------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max;
        if (tab == 0) max = Math.max(0, Store.current().rows().size() - ROWS_G);
        else if (tab == 1) max = Math.max(0, Store.current().payments.size() - ROWS_H);
        else if (tab == 2) max = Math.max(0, Auction.bids.size() - ROWS_B);
        else return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        int ns = Math.max(0, Math.min(max, scroll - (int) Math.signum(verticalAmount)));
        if (ns != scroll) {
            scroll = ns;
            rebuild();
        }
        return true;
    }

    // ---------------- render ----------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Xmas.panel(ctx, px, py, pw, ph);
        Xmas.snow(ctx, px + 2, py + 4, pw - 4, ph - 8, 45);
        switch (tab) {
            case 0: renderGambles(ctx); break;
            case 1: renderHistory(ctx); break;
            case 2: renderAuction(ctx); break;
            case 3: renderTimer(ctx); break;
            case 4: renderCalc(ctx); break;
            case 5: renderGraph(ctx); break;
            default: renderSetup(ctx); break;
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderGambles(DrawContext ctx) {
        Store st = Store.current();
        long net = st.totalNet();
        long today = st.todayNet();
        text(ctx, "Total: " + Money.signed(net), px + 10, py + 32, net >= 0 ? Xmas.POS : Xmas.NEG);
        String t = "Today: " + Money.signed(today);
        text(ctx, t, px + 150, py + 32, today >= 0 ? Xmas.POS : Xmas.NEG);
        String srv = Store.serverKey();
        text(ctx, srv, px + pw - 10 - textRenderer.getWidth(srv), py + 32, Xmas.DIM);

        if (Auction.active) {
            text(ctx, "Auction running - W/L buttons are off", px + 10, py + 66, Xmas.GOLD);
        } else {
            text(ctx, "Pending payments", px + 10, py + 66, Xmas.GOLD);
            List<Store.Payment> pend = st.pending(3);
            if (pend.isEmpty()) text(ctx, "None", px + 10, py + 82, Xmas.DIM);
            for (int i = 0; i < pend.size(); i++) {
                Store.Payment p = pend.get(i);
                text(ctx, p.player + "  " + Money.pretty(p.amount) + "  (pay " + Money.pretty(p.amount * 2) + " if W)",
                        px + 10, py + 83 + i * 16, Xmas.WHITE);
            }
        }

        int hy = py + 138;
        ctx.fill(px + 8, hy - 3, px + pw - 8, hy - 2, 0xFFB8860B);
        text(ctx, "Player", px + 10, hy, Xmas.GOLD);
        text(ctx, "He won (you paid)", px + 120, hy, Xmas.GOLD);
        text(ctx, "He lost", px + 230, hy, Xmas.GOLD);
        text(ctx, "Net", px + 320, hy, Xmas.GOLD);

        List<Store.Row> rows = sortedRows(st);
        for (int i = 0; i < ROWS_G; i++) {
            int idx = i + scroll;
            if (idx >= rows.size()) break;
            Store.Row r = rows.get(idx);
            int y = py + 153 + i * 12;
            text(ctx, r.name, px + 10, y, Xmas.WHITE);
            text(ctx, Money.pretty(r.wPayout) + " (" + r.w + ")", px + 120, y, Xmas.NEG);
            text(ctx, Money.pretty(r.lStake) + " (" + r.l + ")", px + 230, y, Xmas.POS);
            text(ctx, Money.signed(r.net()), px + 320, y, r.net() >= 0 ? Xmas.POS : Xmas.NEG);
        }
    }

    private void renderHistory(DrawContext ctx) {
        Store st = Store.current();
        text(ctx, "Payments (newest first). x removes one payment.", px + 10, py + 32, Xmas.GOLD);
        List<Store.Payment> all = st.newestFirst();
        for (int i = 0; i < ROWS_H; i++) {
            int idx = i + scroll;
            if (idx >= all.size()) break;
            Store.Payment p = all.get(idx);
            int y = py + 49 + i * 14;
            String when = FMT.format(Instant.ofEpochMilli(p.time).atZone(ZoneId.systemDefault()));
            text(ctx, when, px + 10, y, Xmas.DIM);
            text(ctx, p.player, px + 78, y, Xmas.WHITE);
            text(ctx, Money.pretty(p.amount), px + 190, y, Xmas.WHITE);
            String stt = p.status.equals("W") ? "W" : (p.status.equals("L") ? "L" : "...");
            int col = p.status.equals("W") ? Xmas.POS : (p.status.equals("L") ? Xmas.NEG : Xmas.DIM);
            text(ctx, stt, px + 270, y, col);
            if (!p.status.equals("PENDING")) {
                text(ctx, Money.signed(Store.net(p)), px + 300, y, Store.net(p) >= 0 ? Xmas.POS : Xmas.NEG);
            }
        }
        if (all.isEmpty()) text(ctx, "No payments yet.", px + 10, py + 50, Xmas.DIM);
    }

    private void renderAuction(DrawContext ctx) {
        text(ctx, "Item", px + 10, py + 45, Xmas.GOLD);
        text(ctx, "Min bid", px + 10, py + 69, Xmas.GOLD);
        text(ctx, "Seconds", px + 10, py + 93, Xmas.GOLD);
        text(ctx, "(5-120)", px + 166, py + 93, Xmas.DIM);
        text(ctx, "Highest single payment wins. Timer does not reset. W/L buttons are off during auctions.",
                px + 10, py + 142, Xmas.DIM);

        String head = Auction.active
                ? "Running: " + Auction.item + " - " + (Auction.leader == null ? "no bids yet" : Auction.leader + " " + Money.pretty(Auction.leaderAmount))
                : "Bids (highest first)";
        text(ctx, head, px + 10, py + 158, Xmas.GOLD);

        List<Auction.Bid> sorted = new ArrayList<>(Auction.bids);
        sorted.sort((a, b) -> Long.compare(b.amount, a.amount));
        for (int i = 0; i < ROWS_B; i++) {
            int idx = i + scroll;
            if (idx >= sorted.size()) break;
            Auction.Bid b = sorted.get(idx);
            int y = py + 172 + i * 12;
            text(ctx, (idx + 1) + ".", px + 10, y, Xmas.DIM);
            text(ctx, b.name, px + 30, y, idx == 0 ? Xmas.GOLD : Xmas.WHITE);
            text(ctx, Money.pretty(b.amount), px + 160, y, Xmas.POS);
        }
        if (sorted.isEmpty()) text(ctx, "No bids yet.", px + 10, py + 172, Xmas.DIM);
    }

    private void renderTimer(DrawContext ctx) {
        text(ctx, "Timer (max 24 hours)", px + 10, py + 32, Xmas.GOLD);
        text(ctx, "Set time", px + 10, py + 53, Xmas.WHITE);
        text(ctx, "h", px + 143, py + 53, Xmas.DIM);
        text(ctx, "m", px + 193, py + 53, Xmas.DIM);
        text(ctx, "s", px + 243, py + 53, Xmas.DIM);

        String t = "Not running";
        if (Clock.timerSet) {
            t = Clock.timerDone ? "Finished!" : Clock.fmt(Clock.timerRemaining(), true) + (Clock.timerRunning ? " left" : " (paused)");
        }
        text(ctx, t, px + 10, py + 100, Xmas.POS);

        text(ctx, "Stopwatch", px + 10, py + 128, Xmas.GOLD);
        String s = Clock.swSet ? Clock.fmt(Clock.swElapsed(), false) + (Clock.swRunning ? "" : " (paused)") : "Not started";
        text(ctx, s, px + 10, py + 170, Xmas.POS);
    }

    private void renderCalc(DrawContext ctx) {
        text(ctx, "Calculator - type like  2.5m x 2 + 300k", px + 10, py + 32, Xmas.GOLD);
        text(ctx, "Use k m b t, + - x * / and ( )", px + 10, py + 68, Xmas.DIM);
        String expr = fExpr == null ? "" : fExpr.getText();
        lastExpr = expr;
        Double v = Calc.eval(expr);
        String out = expr.isBlank() ? "" : (v == null ? "..." : "= " + Calc.format(v));
        text(ctx, out, px + 10, py + 90, v == null ? Xmas.DIM : Xmas.POS);
    }

    private void renderGraph(DrawContext ctx) {
        Store st = Store.current();
        text(ctx, "Profit over time (since last reset)", px + 10, py + 32, Xmas.GOLD);
        List<Store.Payment> list = st.counted();
        if (list.size() < 2) {
            text(ctx, "Not enough data yet. Mark a few payments W or L.", px + 10, py + 60, Xmas.DIM);
            return;
        }
        int n = list.size() + 1;
        long[] cum = new long[n];
        for (int i = 0; i < list.size(); i++) cum[i + 1] = cum[i] + Store.net(list.get(i));

        long minV = 0, maxV = 0;
        for (long v : cum) {
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
        }
        long span = Math.max(1, maxV - minV);

        int x0 = px + 20, y0 = py + 56, w = pw - 40, h = 160;
        ctx.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, 0xFF0A1A12);
        int yz = y0 + h - (int) ((0 - minV) * (long) h / span);
        ctx.fill(x0, yz, x0 + w, yz + 1, 0x66FFFFFF);

        for (int c = 0; c < w; c++) {
            double t = c * (n - 1) / (double) (w - 1);
            int i = (int) t;
            if (i >= n - 1) i = n - 2;
            double f = t - i;
            double v = cum[i] + (cum[i + 1] - cum[i]) * f;
            int yy = y0 + h - (int) ((v - minV) / span * h);
            yy = Math.max(y0, Math.min(y0 + h - 2, yy));
            ctx.fill(x0 + c, yy, x0 + c + 1, yy + 2, v >= 0 ? Xmas.POS : Xmas.NEG);
        }

        text(ctx, "High " + Money.signed(maxV), px + 10, py + 222, Xmas.POS);
        text(ctx, "Low " + Money.signed(minV), px + 150, py + 222, Xmas.NEG);
        long last = cum[n - 1];
        text(ctx, "Now " + Money.signed(last), px + 280, py + 222, last >= 0 ? Xmas.POS : Xmas.NEG);
    }

    private void renderSetup(DrawContext ctx) {
        Settings se = Settings.get();
        text(ctx, "Sounds", px + 10, py + 32, Xmas.GOLD);
        text(ctx, "Volume", px + 10, py + 48, Xmas.WHITE);
        text(ctx, se.volume + "%", px + 152, py + 48, Xmas.POS);
        text(ctx, "Chat alerts", px + 10, py + 96, Xmas.GOLD);
    }
}
