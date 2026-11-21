package com.pnl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Shows the exact command first. Payouts over the limit need a second confirm. */
public class ConfirmPayScreen extends Screen {
    private final Store.Payment p;
    private final Screen parent;
    private final Store store = Store.current();
    private int step = 1;

    public ConfirmPayScreen(Store.Payment p, Screen parent) {
        super(Text.literal("Confirm payout"));
        this.p = p;
        this.parent = parent;
    }

    private long payout() { return p.amount * 2; }

    private boolean big() { return payout() > store.twoStepOver; }

    private String command() {
        String amt = store.fullNumberPay ? Long.toString(payout()) : Money.format(payout());
        return "pay " + p.player + " " + amt;
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 + 26;
        boolean second = step == 2;
        addDrawableChild(new FancyButton(cx - 115, y, 110, 22, second ? "YES, SEND IT" : (big() ? "Continue" : "Confirm"),
                second ? 0xFFEF4444 : 0xFF22C55E, b -> {
            if (big() && step == 1) {
                step = 2;
                clearAndInit();
                return;
            }
            client.player.networkHandler.sendChatCommand(command());
            store.resolveWin(p);
            Hud.toast("W  paid " + p.player + " " + Money.pretty(payout()), 0x4ADE80, false);
            client.setScreen(parent);
        }));
        addDrawableChild(new FancyButton(cx + 5, y, 110, 22, "Cancel", 0xFF6B7280, b -> client.setScreen(parent)));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xAA000000);
        int cx = width / 2, top = height / 2 - 52;
        ctx.fill(cx - 150, top - 1, cx + 150, top + 105, 0xFF2A3140);
        ctx.fill(cx - 149, top, cx + 149, top + 104, 0xF0141821);

        String title = step == 1 ? "He won. Pay him double?" : "Large payout - are you sure?";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(title), cx, top + 10, step == 1 ? 0xFFFFFFFF : 0xFFF87171);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(p.player + " paid " + Money.pretty(p.amount)), cx, top + 28, 0xFF9CA3AF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("You pay " + Money.pretty(payout())), cx, top + 42, 0xFF4ADE80);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("/" + command()), cx, top + 58, 0xFFFBBF24);
        super.render(ctx, mouseX, mouseY, delta);
    }
}
