package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public class UtilServer {

    /**
     * Shorter version of doing Bukkit.getPluginManager().callEvent()
     *
     * @param event The event to call
     */
    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static void runTask(BPvPPlugin plugin, boolean async, Runnable task) {
        if (plugin.isEnabled()) {
            if (async) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            task.run();
        }

    }

    public static void runTask(BPvPPlugin plugin, Runnable task) {
        runTask(plugin, false, task);
    }

    public static void runTaskAsync(BPvPPlugin plugin, Runnable task) {
        runTask(plugin, true, task);
    }

    public static void runTaskLater(BPvPPlugin plugin, boolean async, Runnable task, long delay) {
        if (plugin.isEnabled()) {
            if (async) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            task.run();
        }
    }

    public static void runTaskLater(BPvPPlugin plugin, Runnable task, long delay) {
        runTaskLater(plugin, false, task, delay);
    }

    public static void runTaskLaterAsync(BPvPPlugin plugin, Runnable task, long delay) {
        runTaskLater(plugin, true, task, delay);
    }

    public static void runTaskTimer(BPvPPlugin plugin, boolean async, Runnable task, long delay, long period) {
        if (plugin.isEnabled()) {
            if (async) {
                Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
            } else {
                Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
            }
        } else {
            task.run();
        }
    }

    public static void runTaskTimer(BPvPPlugin plugin, Runnable task, long delay, long period) {
        runTaskTimer(plugin, false, task, delay, period);
    }

    public static void runTaskTimerAsync(BPvPPlugin plugin, Runnable task, long delay, long period) {
        runTaskTimer(plugin, true, task, delay, period);
    }

}
