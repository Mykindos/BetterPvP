package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitTask;

public class UtilServer {

    /**
     * Shorter version of doing Bukkit.getPluginManager().callEvent()
     *
     * @param event The event to call
     */
    public static <T extends Event> T callEvent(T event) {
        Bukkit.getPluginManager().callEvent(event);
        return event;
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

    public static BukkitTask runTaskLater(BPvPPlugin plugin, boolean async, Runnable task, long delay) {
        if (plugin.isEnabled()) {
            if (async) {
                return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
            } else {
                return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            task.run();
        }

        return null;
    }

    public static BukkitTask runTaskLater(BPvPPlugin plugin, Runnable task, long delay) {
        return runTaskLater(plugin, false, task, delay);
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
