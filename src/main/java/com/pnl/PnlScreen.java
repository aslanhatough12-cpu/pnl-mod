package com.pnl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The K menu: Gambles tab and Auctions tab. */
public class PnlScreen extends Screen {
    private static final int VISIBLE_ROWS = 6;
    private static String lastItem = "";
    private static String lastMin = "1M";
    private static String lastSecs = "60";

    private int tab = 0;
    private int scroll = 0;
    private TextFieldWidget fItem, fMin, fSecs;
    private final int pw = 380, ph = 262;
    private int px, py;

    public PnlScreen() {
        super(Text.literal("PnL"));
    }

    private static String signed(long v) {
        return (v >= 0 ? "+" : "-") + Money.pretty(Math.abs(v));
    }

    @Override
    protected void init() {
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        Store st = Store.current();

        addDrawableChild(new FancyButton(px + 10, py + 8, 90, 18, "Gambles", tab == 0 ? 0xFF3B82F6 : 0xFF4B5563,
                b -> { tab = 0; scroll = 0; clearAndInit(); }));
        addDrawableChild(new FancyButton(px + 106, py + 8, 90, 18, "Auctions", tab == 1 ? 0xFF3B82F6 : 0xFF4B5563,
                b -> { tab = 1; scroll = 0; clearAndInit(); }));

        if (tab == 0) {
            if (!Auction.active) {
                List<Store.Payment> pend = st.pending(4);
                for (int i = 0; i < pend.size(); i++) {
                    Store.Payment p = pend.get(i);
                    int y = py + 64 + i * 20;
                    addDrawableChild(new FancyButton(px + pw - 82, y, 32, 16, "W", 0xFF22C55E,
                            b -> client.setScreen(new ConfirmPayScreen(p, this))));
                    addDrawableChild(new FancyButton(px + pw - 44, y, 32, 16, "L", 0xFFEF4444, b -> {
                        st.resolveLoss(p);
                        Hud.toast("L  " + p.player + " lost " + Money.pretty(p.amount), 0xEF4444, false);
                        clearAndInit();
                    }));
                }
            }
        } else {
            fItem = new TextFieldWidget(textRenderer, px + 100, py + 44, 220, 18, Text.literal("Item"));
            fItem.setMaxLength(40);
            fItem.setText(lastItem);
            addDrawableChild(fItem);

            fMin = new TextFieldWidget(textRenderer, px + 100, py + 70, 120, 18, Text.literal("Min bid"));
            fMin.setMaxLength(16);
            fMin.setText(lastMin);
            addDrawableChild(fMin);

            fSecs = new TextFieldWidget(textRenderer, px + 100, py + 96, 60, 18, Text.literal("Seconds"));
            fSecs.setMaxLength(3);
            fSecs.setText(lastSecs);
            addDrawableChild(fSecs);

            addDrawableChild(new FancyButton(px + 100, py + 128, 110, 22, "Start auction", 0xFF22C55E, b -> startAuction()));
            if (Auction.active) {
                addDrawableChild(new FancyButton(px + 216, py + 128, 104, 22, "Cancel auction", 0xFFEF4444, b -> {
                    Auction.cancel();
                    clearAndInit();
                }));
            }
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
        lastItem = fItem.getText();
        lastMin = fMin.getText();
        lastSecs = fSecs.getText();
        Auction.start(fItem.getText().trim(), min, secs);
        client.setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == 0) {
            int max = Math.max(0, Store.current().stats.size() - VISIBLE_ROWS);
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF2A3140);
        ctx.fill(px, py, px + pw, py + ph, 0xF0141821);
        if (tab == 0) renderGambles(ctx); else renderAuctions(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(s), x, y, color);
    }

    private void renderGambles(DrawContext ctx) {
        Store st = Store.current();
        long net = st.totalNet();
        text(ctx, "Total profit/loss: " + signed(net), px + 10, py + 34, net >= 0 ? 0xFF4ADE80 : 0xFFF87171);
        String srv = Store.serverKey();
        text(ctx, srv, px + pw - 10 - textRenderer.getWidth(srv), py + 14, 0xFF6B7280);

        if (Auction.active) {
            text(ctx, "Auction running - W/L buttons are off", px + 10, py + 52, 0xFFFBBF24);
        } else {
            text(ctx, "Pending payments", px + 10, py + 52, 0xFF9CA3AF);
            List<Store.Payment> pend = st.pending(4);
            if (pend.isEmpty()) text(ctx, "None", px + 10, py + 68, 0xFF6B7280);
            for (int i = 0; i < pend.size(); i++) {
                Store.Payment p = pend.get(i);
                text(ctx, p.player + "  " + Money.pretty(p.amount) + "  (pay " + Money.pretty(p.amount * 2) + " if W)",
                        px + 10, py + 68 + i * 20, 0xFFFFFFFF);
            }
        }

        int hy = py + 150;
        ctx.fill(px + 8, hy - 3, px + pw - 8, hy - 2, 0xFF2A3140);
        text(ctx, "Player", px + 10, hy, 0xFF9CA3AF);
        text(ctx, "He won (you paid)", px + 120, hy, 0xFF9CA3AF);
        text(ctx, "He lost", px + 235, hy, 0xFF9CA3AF);
        text(ctx, "Net", px + 315, hy, 0xFF9CA3AF);

        List<Map.Entry<String, Store.Stat>> rows = new ArrayList<>(st.stats.entrySet());
        rows.sort((a, b) -> Long.compare(b.getValue().wStake + b.getValue().lStake, a.getValue().wStake + a.getValue().lStake));
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scroll;
            if (idx >= rows.size()) break;
            Store.Stat s = rows.get(idx).getValue();
            int y = hy + 14 + i * 14;
            text(ctx, rows.get(idx).getKey(), px + 10, y, 0xFFFFFFFF);
            text(ctx, Money.pretty(s.wPayout) + " (" + s.w + ")", px + 120, y, 0xFFF87171);
            text(ctx, Money.pretty(s.lStake) + " (" + s.l + ")", px + 235, y, 0xFF4ADE80);
            text(ctx, signed(s.net()), px + 315, y, s.net() >= 0 ? 0xFF4ADE80 : 0xFFF87171);
        }
    }

    private void renderAuctions(DrawContext ctx) {
        text(ctx, "Item", px + 10, py + 49, 0xFF9CA3AF);
        text(ctx, "Min bid", px + 10, py + 75, 0xFF9CA3AF);
        text(ctx, "Seconds", px + 10, py + 101, 0xFF9CA3AF);
        text(ctx, "(5-120)", px + 166, py + 101, 0xFF6B7280);
        text(ctx, "Highest single payment wins. Timer does not reset.", px + 10, py + 168, 0xFF6B7280);
        text(ctx, "W/L buttons are off while an auction runs.", px + 10, py + 182, 0xFF6B7280);
        if (Auction.active) {
            String who = Auction.leader == null ? "no bids yet" : Auction.leader + " " + Money.pretty(Auction.leaderAmount);
            text(ctx, "Running: " + Auction.item + " - " + who, px + 10, py + 210, 0xFFFBBF24);
        }
    }
}
