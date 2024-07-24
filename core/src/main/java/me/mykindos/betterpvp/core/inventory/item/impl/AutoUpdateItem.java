package me.mykindos.betterpvp.core.inventory.item.impl;

import me.mykindos.betterpvp.core.inventory.InvUI;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.window.AbstractWindow;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Supplier;

/**
 * An {@link Item} that updates its {@link ItemProvider} every specified amount
 * of ticks.
 */
public class AutoUpdateItem extends SuppliedItem {
    
    private final int period;
    private BukkitTask task;
    
    public AutoUpdateItem(int period, Supplier<? extends ItemProvider> builderSupplier) {
        super(builderSupplier, null);
        this.period = period;
    }
    
    public void start() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(InvUI.getInstance().getPlugin(), this::notifyWindows, 0, period);
    }
    
    public void cancel() {
        task.cancel();
        task = null;
    }
    
    @Override
    public void addWindow(AbstractWindow window) {
        super.addWindow(window);
        if (task == null) start();
    }
    
    @Override
    public void removeWindow(AbstractWindow window) {
        super.removeWindow(window);
        if (getWindows().isEmpty() && task != null) cancel();
    }
    
}
