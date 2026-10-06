package me.mykindos.betterpvp.core.menu.dialog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.Bukkit;

/**
 * Schedules dialog work on the server thread.
 */
@Singleton
public class BukkitDialogScheduler implements DialogScheduler {

    private final Core core;

    @Inject
    public BukkitDialogScheduler(Core core) {
        this.core = core;
    }

    @Override
    public void later(int ticks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(core, task, ticks);
    }
}
