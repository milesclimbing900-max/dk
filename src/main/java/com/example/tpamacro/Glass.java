package com.example.tpamacro;

import net.minecraft.client.gui.DrawContext;

/** Liquid-glass style drawing helpers (rounded, translucent, specular rims). */
public final class Glass {
    private Glass() {}

    public static int rgba(int argb, float alphaMul) {
        int a = Math.round(((argb >>> 24) & 0xFF) * alphaMul);
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int inset(int r, int row, int h) {
        if (r <= 0) return 0;
        double dy;
        if (row < r) dy = r - (row + 0.5);
        else if (row >= h - r) dy = (row + 0.5) - (h - r);
        else return 0;
        double v = (double) r * r - dy * dy;
        if (v < 0) v = 0;
        return (int) Math.round(r - Math.sqrt(v));
    }

    /** Filled rounded rect with a vertical gradient. */
    public static void fill(DrawContext c, int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < r && row < h; row++) {
            int i = inset(r, row, h);
            c.fill(x + i, y + row, x + w - i, y + row + 1, lerp(top, bottom, (row + 0.5f) / h));
        }
        int midTop = r, midBot = h - r;
        if (midBot > midTop) {
            c.fillGradient(x, y + midTop, x + w, y + midBot,
                    lerp(top, bottom, (float) midTop / h), lerp(top, bottom, (float) midBot / h));
        }
        for (int row = Math.max(h - r, r); row < h; row++) {
            int i = inset(r, row, h);
            c.fill(x + i, y + row, x + w - i, y + row + 1, lerp(top, bottom, (row + 0.5f) / h));
        }
    }

    /** 1px rounded outline, brighter on the top-left, fading to the bottom-right. */
    public static void stroke(DrawContext c, int x, int y, int w, int h, int r, int tl, int br) {
        if (w <= 2 || h <= 2) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            float t = (row + 0.5f) / h;
            int io = inset(r, row, h);
            if (row == 0 || row == h - 1) {
                c.fill(x + io, y + row, x + w - io, y + row + 1, lerp(tl, br, row == 0 ? 0.15f : 0.85f));
                continue;
            }
            int ii = Math.max(io + 1, 1 + inset(Math.max(0, r - 1), row - 1, h - 2));
            int cl = lerp(tl, br, t * 0.7f);
            int cr = lerp(tl, br, 0.3f + t * 0.7f);
            c.fill(x + io, y + row, x + ii, y + row + 1, cl);
            c.fill(x + w - ii, y + row, x + w - io, y + row + 1, cr);
        }
    }

    /** Big frosted glass panel with soft shadow and a sheen on top. */
    public static void panel(DrawContext c, int x, int y, int w, int h, int r) {
        for (int i = 4; i >= 1; i--) {
            int e = i * 3;
            fill(c, x - e, y - e + 5, w + 2 * e, h + 2 * e, r + e, 0x0A000000, 0x10000000);
        }
        fill(c, x, y, w, h, r, 0x48E6EEFF, 0x1EC8D2FF);
        fill(c, x + 3, y + 2, w - 6, h / 3, Math.max(2, r - 3), 0x30FFFFFF, 0x00FFFFFF);
        stroke(c, x, y, w, h, r, 0xC8FFFFFF, 0x30FFFFFF);
    }

    /** Glass capsule button / chip / selected segment. tint is 0xRRGGBB. */
    public static void pill(DrawContext c, int x, int y, int w, int h, int tint, float strength, boolean hover) {
        int ta = Math.min(255, (int) (0x78 * strength) + (hover ? 0x20 : 0));
        int ba = Math.min(255, (int) (0x44 * strength) + (hover ? 0x14 : 0));
        fill(c, x, y, w, h, h / 2, (ta << 24) | (tint & 0xFFFFFF), (ba << 24) | (tint & 0xFFFFFF));
        fill(c, x + 2, y + 1, w - 4, h / 2, h / 4, 0x3CFFFFFF, 0x00FFFFFF);
        stroke(c, x, y, w, h, h / 2, 0xB4FFFFFF, 0x30FFFFFF);
    }

    /** Sunken capsule used for tracks and text fields. */
    public static void well(DrawContext c, int x, int y, int w, int h) {
        fill(c, x, y, w, h, h / 2, 0x58000000, 0x34000000);
        stroke(c, x, y, w, h, h / 2, 0x18FFFFFF, 0x44FFFFFF);
    }
}
