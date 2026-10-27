package com.example.tpamacro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayDeque;

public final class Macro {
    public static boolean running;

    private static final ArrayDeque<String> queue = new ArrayDeque<>();
    private static int cooldown = 0;
    private static int cycleTicks = 1;

    private Macro() {}

    public static float secondsLeft() { return Math.max(0, cooldown) / 20f; }

    public static float progress() {
        return 1f - Math.max(0, cooldown) / (float) Math.max(1, cycleTicks);
    }

    public static void toggle(MinecraftClient c) {
        if (running) stop(c, true);
        else start(c);
    }

    public static void start(MinecraftClient c) {
        if (c.player == null) return;
        if (Config.targets.isEmpty()) {
            Island.pop("Add a streamer first", "Open the menu with Right Shift", 0xFF9F0A, 3000);
            return;
        }
        for (Config.Target t : Config.targets) t.got = false;
        queue.clear();
        cooldown = 0;
        cycleTicks = 1;
        running = true;
    }

    public static void stop(MinecraftClient c, boolean notify) {
        boolean was = running;
        running = false;
        queue.clear();
        if (was && notify) Island.pop("Macro stopped", "", 0x8E8E93, 1800);
    }

    public static void tick(MinecraftClient c) {
        if (!running) return;
        if (c.player == null || c.world == null) {
            running = false;
            queue.clear();
            return;
        }

        checkArrivals(c);
        if (!running) return;

        // one command per tick so a big list never bursts in a single tick
        String next = queue.poll();
        if (next != null) c.player.networkHandler.sendChatCommand(next);

        if (--cooldown <= 0) {
            if (queue.isEmpty()) {
                for (Config.Target t : Config.targets) {
                    if (!t.got) queue.add(Config.mode + " " + t.name);
                }
            }
            cycleTicks = Math.max(1, (int) Math.round(Config.interval * 20.0));
            cooldown = cycleTicks;
        }
    }

    private static String norm(String s) {
        return s.startsWith(".") ? s.substring(1) : s;
    }

    private static void checkArrivals(MinecraftClient c) {
        double r2 = Config.range * Config.range;

        for (Config.Target t : Config.targets) {
            if (t.got) continue;
            for (PlayerEntity p : c.world.getPlayers()) {
                if (p == c.player) continue;
                if (!norm(p.getName().getString()).equalsIgnoreCase(norm(t.name))) continue;
                if (p.squaredDistanceTo(c.player) > r2) continue;

                t.got = true;
                queue.removeIf(cmd -> cmd.equalsIgnoreCase(Config.mode + " " + t.name));
                c.player.sendMessage(Text.literal("[TpaMacro] Got " + t.name), false);

                int left = 0;
                for (Config.Target o : Config.targets) if (!o.got) left++;

                if (Config.stopMode == Config.StopMode.FIRST) {
                    Island.pop("Got " + t.name, "Macro stopped", 0x34C759, 4000);
                    stop(c, false);
                    return;
                }
                if (left == 0) {
                    Island.pop("Got " + t.name, "That's everyone. Macro stopped", 0x34C759, 4000);
                    stop(c, false);
                    return;
                }
                Island.pop("Got " + t.name, left + " left", 0x34C759, 2500);
                break;
            }
        }
    }
}
