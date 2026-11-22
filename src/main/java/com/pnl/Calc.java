package com.pnl;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tiny calculator: 2.5m x 2 + 300k, ( ), + - x * / and k m b t suffixes. */
public final class Calc {
    private final String s;
    private int pos = 0;

    private Calc(String s) {
        this.s = s;
    }

    /** Returns null if the text can't be calculated. */
    public static Double eval(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            Calc c = new Calc(text.toLowerCase());
            double v = c.expr();
            c.ws();
            if (c.pos != c.s.length()) return null;
            if (Double.isNaN(v) || Double.isInfinite(v)) return null;
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    public static String format(double v) {
        if (Math.abs(v) >= 1000) return Money.pretty(Math.round(v));
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private void ws() {
        while (pos < s.length() && s.charAt(pos) == ' ') pos++;
    }

    private boolean match(char c) {
        ws();
        if (pos < s.length() && s.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private double expr() {
        double v = term();
        while (true) {
            if (match('+')) v += term();
            else if (match('-')) v -= term();
            else return v;
        }
    }

    private double term() {
        double v = factor();
        while (true) {
            if (match('*') || match('x') || match('\u00d7')) v *= factor();
            else if (match('/')) v /= factor();
            else return v;
        }
    }

    private double factor() {
        if (match('-')) return -factor();
        if (match('+')) return factor();
        if (match('(')) {
            double v = expr();
            if (!match(')')) throw new RuntimeException("missing )");
            return v;
        }
        return number();
    }

    private double number() {
        ws();
        int st = pos;
        while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.' || s.charAt(pos) == ',')) pos++;
        if (st == pos) throw new RuntimeException("number expected");
        double v = Double.parseDouble(s.substring(st, pos).replace(",", ""));
        if (pos < s.length()) {
            char u = s.charAt(pos);
            double m = 1;
            if (u == 'k') m = 1e3;
            else if (u == 'm') m = 1e6;
            else if (u == 'b') m = 1e9;
            else if (u == 't') m = 1e12;
            if (m != 1) {
                pos++;
                v *= m;
            }
        }
        return v;
    }
}
