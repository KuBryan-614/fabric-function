package kuku.modslash;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class TickTracker {

    public static final long[] TICK_TIMES = new long[100];
    private static int index = 0;
    private static long start = 0;

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            start = System.nanoTime();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TICK_TIMES[index] = System.nanoTime() - start;
            index = (index + 1) % 100;
        });
    }
}