package com.example.tpamacro;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class MacroScreen extends Screen {
    private static final String[] MODES = {"/tpahere", "/tpa"};
    private static final String[] STOPS = {"Keep going", "First", "All"};
    private static final int KNOB_W = 26;

    private TextFieldWidget field;
    private boolean prevDown, prevEnter;
    private int dragging = 0; // 0 none, 1 delay, 2 range
    private float modeAnim, stopAnim;
    private long lastNs;

    public MacroScreen() {
        super(Text.literal("TPA Macro"));
        modeAnim = Config.mode.equals("tpa") ? 1 : 0;
        stopAnim = Config.stopMode.ordinal();
    }

    @Override
    protected void init() {
        field = new TextFieldWidget(this.textRenderer, 0, 0, 100, 10, Text.literal("Streamer names"));
        field.setDrawsBackground(false);
        field.setMaxLength(100);
        field.setEditableColor(0xFFFFFFFF);
        field.setTextPredicate(s -> s.matches("[A-Za-z0-9_., ]*"));
        this.addSelectableChild(field);
        this.setInitialFocus(field);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void label(DrawContext c, String s, int x, int y) {
        c.drawText(textRenderer, s, x, y, 0xC8FFFFFF, false);
    }

    private void centered(DrawContext c, Text t, int cx, int y, int color) {
        c.drawText(textRenderer, t, cx - textRenderer.getWidth(t) / 2, y, color, false);
    }

    private int segmented(DrawContext c, int x, int y, int w, int h, String[] labels, float anim, int mx, int my) {
        Glass.well(c, x, y, w, h);
        int n = labels.length;
        float sw = w / (float) n;
        int tx = x + 2 + Math.round(anim * sw);
        Glass.pill(c, tx, y + 2, Math.round(sw) - 4, h - 4, 0xFFFFFF, 1f, false);
        for (int i = 0; i < n; i++) {
            int cx = x + Math.round(sw * i + sw / 2);
            boolean sel = Math.abs(anim - i) < 0.5f;
            Text t = sel ? Text.literal(labels[i]).formatted(Formatting.BOLD) : Text.literal(labels[i]);
            centered(c, t, cx, y + (h - 8) / 2, sel ? 0xFFFFFFFF : 0xB4FFFFFF);
        }
        return in(mx, my, x, y, w, h) ? Math.min(n - 1, (int) ((mx - x) / sw)) : -1;
    }

    private void slider(DrawContext c, int x, int y, int w, float frac) {
        int ty = y + 6;
        Glass.well(c, x, ty, w, 6);
        int kx = x + Math.round(frac * (w - KNOB_W));
        int fillW = kx - x + KNOB_W / 2;
        Glass.fill(c, x, ty, fillW, 6, 3, 0xF04DA3FF, 0xF02D7DFF);
        Glass.fill(c, kx - 1, y + 3, KNOB_W + 2, 18, 9, 0x22000000, 0x32000000);
        Glass.fill(c, kx, y + 1, KNOB_W, 16, 8, 0xFFFFFFFF, 0xFFE2E7F2);
        Glass.stroke(c, kx, y + 1, KNOB_W, 16, 8, 0x60FFFFFF, 0x28000000);
    }

    private double fracFromMouse(int mx, int sx, int sw) {
        return Math.max(0, Math.min(1, (mx - sx - KNOB_W / 2.0) / (sw - KNOB_W)));
    }

    private void addNames() {
        for (String n : field.getText().trim().split("[\\s,]+")) {
            if (!n.isBlank()) Config.addTarget(n);
        }
        field.setText("");
        Config.save();
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        try {
    renderBackground(c, mx, my, delta);
} catch (IllegalStateException ignored) {
    // blur was already applied this frame, so the background is already blurred
}

        long nowNs = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (nowNs - lastNs) / 1_000_000_000f);
        lastNs = nowNs;

        // input is polled so we don't depend on the 1.21.9+ click/key event classes
        long win = GLFW.glfwGetCurrentContext();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean click = down && !prevDown;
        boolean released = !down && prevDown;
        prevDown = down;
        boolean enterNow = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_KP_ENTER) == GLFW.GLFW_PRESS;
        boolean enter = enterNow && !prevEnter;
        prevEnter = enterNow;

        float k = 1f - (float) Math.exp(-dt * 16f);
        modeAnim += ((Config.mode.equals("tpa") ? 1 : 0) - modeAnim) * k;
        stopAnim += (Config.stopMode.ordinal() - stopAnim) * k;

        // ---- layout ----
        final int pw = 300, iw = pw - 28;
        int n = Math.min(Config.targets.size(), 30);
        int[] chX = new int[n], chY = new int[n], chW = new int[n];
        int curX = 0, row = 0;
        for (int i = 0; i < n; i++) {
            int cw = textRenderer.getWidth(Config.targets.get(i).name) + 28;
            if (curX > 0 && curX + cw > iw) { curX = 0; row++; }
            chX[i] = curX; chY[i] = row * 21; chW[i] = cw;
            curX += cw + 5;
        }
        int rows = n == 0 ? 0 : row + 1;
        int chipH = rows * 21 + (rows > 0 ? 4 : 0);
        int ph = 222 + chipH;
        int px = (this.width - pw) / 2;
        int py = Math.max(52, (this.height - ph) / 2 + 12);
        int lx = px + 14;
        int cx0 = px + 14 + 74;
        int cwid = pw - 28 - 74;
        int r0 = py + 14 + 22;
        int sliderW = cwid - 56;

        Glass.panel(c, px, py, pw, ph, 20);

        // ---- title ----
        c.drawText(textRenderer, Text.literal("TPA Macro").formatted(Formatting.BOLD), lx, py + 17, 0xFFFFFFFF, false);
        String st = Macro.running ? "Running" : "Idle";
        int stW = textRenderer.getWidth(st);
        int stX = px + pw - 14 - stW;
        c.drawText(textRenderer, st, stX, py + 17, Macro.running ? 0xFF34C759 : 0xFF9A9AA0, false);
        int dotCol = Macro.running ? 0xFF34C759 : 0xFF8E8E93;
        Glass.fill(c, stX - 11, py + 18, 6, 6, 3, dotCol, dotCol);

        // ---- mode ----
        label(c, "Mode", lx, r0 + 7);
        int hm = segmented(c, cx0, r0, cwid, 22, MODES, modeAnim, mx, my);
        if (click && hm >= 0) { Config.mode = hm == 0 ? "tpahere" : "tpa"; Config.save(); }

        // ---- delay ----
        int dy = r0 + 28;
        label(c, "Delay", lx, dy + 7);
        slider(c, cx0, dy + 2, sliderW, (float) (Config.interval / 10.0));
        String dv = Config.interval < 0.05 ? "Instant" : String.format(Locale.ROOT, "%.1fs", Config.interval);
        int dvCol = Config.interval < 0.25 ? 0xFFFF9F0A : 0xFFFFFFFF;
        c.drawText(textRenderer, dv, cx0 + cwid - textRenderer.getWidth(dv), dy + 7, dvCol, false);
        if (click && in(mx, my, cx0, dy, sliderW, 22)) dragging = 1;

        // ---- range ----
        int ry2 = r0 + 56;
        label(c, "Range", lx, ry2 + 7);
        slider(c, cx0, ry2 + 2, sliderW, (float) ((Config.range - 1.0) / 15.0));
        String rv = String.format(Locale.ROOT, "%.1f blocks", Config.range);
        c.drawText(textRenderer, rv, cx0 + cwid - textRenderer.getWidth(rv), ry2 + 7, 0xFFFFFFFF, false);
        if (click && in(mx, my, cx0, ry2, sliderW, 22)) dragging = 2;

        if (down && dragging != 0) {
            double f = fracFromMouse(mx, cx0, sliderW);
            if (dragging == 1) Config.interval = Math.round(f * 100) / 10.0;
            else Config.range = 1.0 + Math.round(f * 30) / 2.0;
        }
        if (!down) dragging = 0;
        if (released) Config.save();

        // ---- auto-stop ----
        int sy = r0 + 84;
        label(c, "Auto-stop", lx, sy + 7);
        int hs = segmented(c, cx0, sy, cwid, 22, STOPS, stopAnim, mx, my);
        if (click && hs >= 0) { Config.stopMode = Config.StopMode.values()[hs]; Config.save(); }

        // ---- streamers input ----
        int fy = r0 + 112;
        int fieldW = iw - 54;
        Glass.well(c, lx, fy, fieldW, 22);
        field.setX(lx + 11);
        field.setY(fy + 7);
        field.setWidth(fieldW - 22);
        field.render(c, mx, my, delta);
        if (field.getText().isEmpty() && !field.isFocused()) {
            c.drawText(textRenderer, "Streamer names (space separated)", lx + 11, fy + 7, 0x66FFFFFF, false);
        }
        int ax = lx + iw - 48;
        boolean addHover = in(mx, my, ax, fy, 48, 22);
        Glass.pill(c, ax, fy, 48, 22, 0x0A84FF, 1.3f, addHover);
        centered(c, Text.literal("Add").formatted(Formatting.BOLD), ax + 24, fy + 7, 0xFFFFFFFF);
        if (click && addHover) addNames();
        else if (click && in(mx, my, lx, fy, fieldW, 22)) { this.setFocused(field); field.setFocused(true); }
        if (enter && field.isFocused()) addNames();

        // ---- chips ----
        int cy = fy + 28;
        int removeIdx = -1;
        for (int i = 0; i < n; i++) {
            Config.Target t = Config.targets.get(i);
            int x = lx + chX[i], y = cy + chY[i], w = chW[i];
            boolean hov = in(mx, my, x, y, w, 18);
            if (t.got) Glass.pill(c, x, y, w, 18, 0x34C759, 1.1f, hov);
            else Glass.pill(c, x, y, w, 18, 0xFFFFFF, 0.7f, hov);
            c.drawText(textRenderer, t.name, x + 10, y + 5, 0xFFFFFFFF, false);
            c.drawText(textRenderer, t.got ? "✓" : "×", x + w - 14, y + 5, 0xFFFFFFFF, false);
            if (click && in(mx, my, x + w - 20, y, 20, 18)) removeIdx = i;
        }
        if (removeIdx >= 0) { Config.targets.remove(removeIdx); Config.save(); }

        // ---- start / stop ----
        int by = py + 184 + chipH;
        boolean bh = in(mx, my, lx, by, iw, 24);
        Glass.pill(c, lx, by, iw, 24, Macro.running ? 0xFF453A : 0x34C759, 1.5f, bh);
        centered(c, Text.literal(Macro.running ? "Stop" : "Start").formatted(Formatting.BOLD),
                lx + iw / 2, by + 8, 0xFFFFFFFF);
        if (click && bh) Macro.toggle(this.client);

        // Dynamic Island above everything
        Island.render(c, this.client);
    }
}
