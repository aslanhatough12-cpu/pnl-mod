package com.pnl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PnlMod implements ClientModInitializer {
    // Matches: "You have been paid 2.4M from SnagMC."  (names may start with a dot)
    private static final Pattern PAID_ME =
            Pattern.compile("^You have been paid (.+?) from (\\S+?)\\.?$", Pattern.CASE_INSENSITIVE);

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.pnl.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                if (mc.player != null) {
                    mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP.value(), 1.0f, 1.3f);
                }
                if (mc.currentScreen == null && mc.player != null) {
                    mc.setScreen(new PnlScreen());
                }
            }
            Auction.tick(mc);
        });

        ClientReceiveMessageEvents.MODIFY_GAME.register(PnlMod::onMessage);
        HudRenderCallback.EVENT.register(Hud::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("pnlmod")
                        .then(ClientCommandManager.literal("w")
                                .then(ClientCommandManager.argument("id", IntegerArgumentType.integer())
                                        .executes(ctx -> { handle(true, IntegerArgumentType.getInteger(ctx, "id")); return 1; })))
                        .then(ClientCommandManager.literal("l")
                                .then(ClientCommandManager.argument("id", IntegerArgumentType.integer())
                                        .executes(ctx -> { handle(false, IntegerArgumentType.getInteger(ctx, "id")); return 1; })))
                        .then(ClientCommandManager.literal("open")
                                .executes(ctx -> {
                                    MinecraftClient mc = MinecraftClient.getInstance();
                                    mc.send(() -> mc.setScreen(new PnlScreen()));
                                    return 1;
                                }))));
    }

    private static Text onMessage(Text message, boolean overlay) {
        if (overlay) return message;
        Matcher m = PAID_ME.matcher(message.getString().trim());
        if (!m.matches()) return message;

        long amount = Money.parse(m.group(1));
        String name = m.group(2);
        if (amount <= 0) return message;

        Hud.toast(name + " paid you " + Money.pretty(amount), 0x4ADE80, true);

        // During an auction payments are bids, not gambles: no W/L buttons.
        if (Auction.active) {
            Auction.bid(name, amount);
            return message;
        }

        Store.Payment p = Store.current().addPayment(name, amount);
        Text w = button("[W]", Formatting.GREEN, "pnlmod w " + p.id,
                "He won - pay him " + Money.pretty(amount * 2));
        Text l = button("[L]", Formatting.RED, "pnlmod l " + p.id,
                "He lost - keep " + Money.pretty(amount));
        return Text.empty().append(message).append(Text.literal("  ")).append(w).append(Text.literal(" ")).append(l);
    }

    private static Text button(String label, Formatting color, String command, String tip) {
        return Text.literal(label).setStyle(Style.EMPTY
                .withColor(color)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal(tip))));
    }

    private static void handle(boolean win, int id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Store st = Store.current();
        Store.Payment p = st.find(id);
        if (p == null || !p.status.equals("PENDING")) {
            Hud.toast("That payment was already handled", 0xFBBF24, false);
            return;
        }
        if (win) {
            mc.send(() -> mc.setScreen(new ConfirmPayScreen(p, null)));
        } else {
            st.resolveLoss(p);
            if (mc.player != null) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.4f);
            Hud.toast("L  " + p.player + " lost " + Money.pretty(p.amount), 0xEF4444, false);
        }
    }
}
