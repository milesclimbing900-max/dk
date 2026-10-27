package com.example.tpamacro;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Config {
    public enum StopMode { KEEP, FIRST, ALL }

    public static final class Target {
        public final String name;
        public boolean got;
        public Target(String name) { this.name = name; }
    }

    public static final List<Target> targets = new ArrayList<>();
    public static String mode = "tpahere";          // "tpa" or "tpahere"
    public static double interval = 3.0;            // 0 - 10 seconds
    public static double range = 6.0;               // blocks, 1 - 16
    public static StopMode stopMode = StopMode.FIRST;

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("tpamacro.properties");
    }

    public static boolean addTarget(String name) {
        for (Target t : targets) if (t.name.equalsIgnoreCase(name)) return false;
        if (targets.size() >= 30) return false;
        targets.add(new Target(name));
        return true;
    }

    public static void load() {
        Path p = file();
        if (!Files.exists(p)) return;
        Properties pr = new Properties();
        try (Reader r = Files.newBufferedReader(p)) {
            pr.load(r);
            mode = "tpa".equals(pr.getProperty("mode")) ? "tpa" : "tpahere";
            interval = clamp(Double.parseDouble(pr.getProperty("interval", "3.0")), 0, 10);
            range = clamp(Double.parseDouble(pr.getProperty("range", "6.0")), 1, 16);
            try {
                stopMode = StopMode.valueOf(pr.getProperty("stop", "FIRST"));
            } catch (IllegalArgumentException e) {
                stopMode = StopMode.FIRST;
            }
            targets.clear();
            for (String n : pr.getProperty("targets", "").split(",")) {
                if (!n.isBlank()) addTarget(n.trim());
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        Properties pr = new Properties();
        pr.setProperty("mode", mode);
        pr.setProperty("interval", Double.toString(interval));
        pr.setProperty("range", Double.toString(range));
        pr.setProperty("stop", stopMode.name());
        StringBuilder sb = new StringBuilder();
        for (Target t : targets) {
            if (sb.length() > 0) sb.append(',');
            sb.append(t.name);
        }
        pr.setProperty("targets", sb.toString());
        try (Writer w = Files.newBufferedWriter(file())) {
            pr.store(w, "TPA Macro");
        } catch (Exception ignored) {
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
