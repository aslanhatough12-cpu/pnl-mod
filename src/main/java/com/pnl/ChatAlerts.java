package com.pnl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Popups for private messages and for someone saying your name in chat. */
public final class ChatAlerts {
    // "anticONMC -> You: yo what?"  (optional [Rank] tags in front are ignored)
    private static final Pattern DM_IN =
            Pattern.compile("^(?:\\[[^\\]]*\\]\\s*)*(\\S+)\\s*->\\s*You\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // "You -> anticONMC: hello"  (your own message, ignored)
    private static final Pattern DM_OUT =
            Pattern.compile("^(?:\\[[^\\]]*\\]\\s*)*You\\s*->\\s*(\\S+)\\s*:.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // normal vanilla chat looks like "<Name> message"
    private static final Pattern VANILLA = Pattern.compile("^<([^>]{1,32})>");
    private static final String ME = "mrmrblack";

    private ChatAlerts() {}

    public static void handle(String raw) {
        if (raw == null) return;
        String s = raw.trim();
        if (s.isEmpty()) return;
        Settings se = Settings.get();

        if (DM_OUT.matcher(s).matches()) return;

        Matcher in = DM_IN.matcher(s);
        if (in.matches()) {
            if (se.dmAlerts) Hud.toast(in.group(1) + " has messaged you in chat", 0xC77DFF, true);
            return;
        }

        if (!se.mentionAlerts) return;
        String low = s.toLowerCase();
        if (low.startsWith("you have been paid") || low.startsWith("you have paid") || low.startsWith("[pnl]")) return;
        if (!low.contains(ME)) return;

        Matcher v = VANILLA.matcher(s);
        String who = v.find() ? v.group(1) : "Someone";
        Hud.toast(who + " has mentioned you in chat", 0xFFD700, true);
    }
}
