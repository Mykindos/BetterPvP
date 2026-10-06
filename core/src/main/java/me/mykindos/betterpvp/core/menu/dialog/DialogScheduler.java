package me.mykindos.betterpvp.core.menu.dialog;

import com.google.inject.ImplementedBy;

/**
 * Runs dialog work a few ticks later, such as settling a pressed button back to normal.
 */
@ImplementedBy(BukkitDialogScheduler.class)
public interface DialogScheduler {

    void later(int ticks, Runnable task);
}
