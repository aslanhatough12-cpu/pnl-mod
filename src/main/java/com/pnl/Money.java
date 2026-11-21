package com.pnl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and formats amounts like 2.4M, 1.1 mil, 500k, 3B, 1.2T. */
public final class Money {
    private static final Pattern P = Pattern.compile("^([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([a-zA-Z]*)$");

    private Money() {}

    /** Returns the amount as a whole number, or -1 if it can't be read. */
    public static long parse(String s) {
        if (s == null) return -1;
        Matcher m = P.matcher(s.trim());
        if (!m.matches()) return -1;
        long mult;
        switch (m.group(2).toLowerCase()) {
            case "": mult = 1L; break;
            case "k": mult = 1_000L; break;
            case "m": case "mil": case "million": mult = 1_000_000L; break;
            case "b": case "bil": case "billion": mult = 1_000_000_000L; break;
            case "t": case "tril": case "trillion": mult = 1_000_000_000_000L; break;
            default: return -1;
        }
        try {
            BigDecimal v = new BigDecimal(m.group(1).replace(",", "")).multiply(BigDecimal.valueOf(mult));
            return v.setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Exact short form used in the /pay command, e.g. 4260000 -> 4.26M */
    public static String format(long v) {
        long a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a >= 1_000_000_000_000L) return sign + trim(a, 12) + "T";
        if (a >= 1_000_000_000L) return sign + trim(a, 9) + "B";
        if (a >= 1_000_000L) return sign + trim(a, 6) + "M";
        if (a >= 1_000L) return sign + trim(a, 3) + "K";
        return Long.toString(v);
    }

    /** Rounded to 2 decimals, for display only. */
    public static String pretty(long v) {
        long a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a >= 1_000_000_000_000L) return sign + round(a, 12) + "T";
        if (a >= 1_000_000_000L) return sign + round(a, 9) + "B";
        if (a >= 1_000_000L) return sign + round(a, 6) + "M";
        if (a >= 1_000L) return sign + round(a, 3) + "K";
        return Long.toString(v);
    }

    private static String trim(long a, int pow) {
        return new BigDecimal(a).movePointLeft(pow).stripTrailingZeros().toPlainString();
    }

    private static String round(long a, int pow) {
        return new BigDecimal(a).movePointLeft(pow).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
