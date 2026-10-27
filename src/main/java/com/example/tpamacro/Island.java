package com.example.tpamacro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

/** Dynamic-Island style pill at the top of the screen with spring animation. */
public final class Island {
    private static String popTitle = "";
    private static String popSub = "";
    private static int popColor = 0x34C759;
    private static long popUntil = 0;

    private static float w = 44, h = 14, vw = 0, vh = 0, alpha = 0;
    private static long lastNs = 0;

    private Island() {}

    public static void pop(String title, String sub, int rgb, long ms) {
        popTitle = title;
        popSub = sub == null ? "" : sub;
        popColor = rgb;
        popUntil = System.currentTimeMillis() + ms;
    }

    private static String summary() {
        StringBuilder sb = new StringBuilder();
        for (Config.Target t : Config.targets) {
            if (t.got) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.name);
        }
        String s = sb.toString();
        return s.length() > 22 ? s.substring(0, 21) + "…" : s;
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        TextRenderer tr = mc.textRenderer;
        long nowNs = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (nowNs - lastNs) / 1_000_000_000f);
        lastNs = nowNs;

        boolean event = System.currentTimeMillis() < popUntil;
        boolean run = Macro.running;

        String left = "";
        String right = "";
        float tw, th, ta;
        if (event) {
            tw = Math.max(150, Math.max(tr.getWidth(popTitle), tr.getWidth(popSub)) + 52);
            th = popSub.isEmpty() ? 28 : 38;
            ta = 1;
        } else if (run) {
            left = "/" + Config.mode + "  " + summary();
            right = String.format(Locale.ROOT, "%.1fs", Macro.secondsLeft());
            tw = tr.getWidth(left) + tr.getWidth("00.0s") + 56;
            th = 28;
            ta = 1;
        } else {
            tw = 44;
            th = 14;
            ta = 0;
        }

        // springs (slightly underdamped = the little Dynamic Island bounce)
        vw += ((tw - w) * 380f - vw * 24f) * dt;
        w += vw * dt;
        vh += ((th - h) * 380f - vh * 24f) * dt;
        h += vh * dt;
        alpha += (ta - alpha) * (1f - (float) Math.exp(-dt * 12f));
        if (alpha < 0.02f) return;

        int iw = Math.max(24, Math.round(w));
        int ih = Math.max(12, Math.round(h));
        int x = c.getScaledWindowWidth() / 2 - iw / 2;
        int y = 6;
        float a = Math.min(1f, alpha);

        if (event) {
            for (int i = 4; i >= 1; i--) {
                int col = (0x16 << 24) | (popColor & 0xFFFFFF);
                Glass.fill(c, x - i * 2, y - i * 2, iw + i * 4, ih + i * 4, ih / 2 + i * 2,
                        Glass.rgba(col, a), Glass.rgba(col, a));
            }
        }

        int body = Glass.rgba(0xF4060606, a);
        Glass.fill(c, x, y, iw, ih, ih / 2, body, body);
        Glass.stroke(c, x, y, iw, ih, ih / 2, Glass.rgba(0x3CFFFFFF, a), Glass.rgba(0x12FFFFFF, a));

        if (alpha > 0.6f && iw > tw * 0.8f) {
            c.enableScissor(x + 4, y, x + iw - 4, y + ih);
            if (event) {
                Glass.fill(c, x + 13, y + ih / 2 - 4, 8, 8, 4, 0xFF000000 | popColor, 0xFF000000 | popColor);
                int ty = popSub.isEmpty() ? y + (ih - 8) / 2 : y + 8;
                c.drawText(tr, Text.literal(popTitle).formatted(Formatting.BOLD), x + 29, ty, 0xFFFFFFFF, false);
                if (!popSub.isEmpty()) c.drawText(tr, popSub, x + 29, y + 21, 0xFF9A9AA0, false);
            } else if (run) {
                float pulse = 0.55f + 0.45f * (float) Math.sin(nowNs / 1.8e8);
                Glass.fill(c, x + 13, y + 7, 6, 6, 3,
                        Glass.rgba(0xFF34C759, pulse), Glass.rgba(0xFF34C759, pulse));
                c.drawText(tr, left, x + 26, y + 6, 0xFFFFFFFF, false);
                c.drawText(tr, right, x + iw - 14 - tr.getWidth(right), y + 6, 0xFFB0B0B8, false);
                int bx = x + 14, bw = iw - 28, by = y + ih - 6;
                c.fill(bx, by, bx + bw, by + 2, 0x30FFFFFF);
                c.fill(bx, by, bx + Math.round(bw * Macro.progress()), by + 2, 0xFF34C759);
            }
            c.disableScissor();
        }
    }
}
