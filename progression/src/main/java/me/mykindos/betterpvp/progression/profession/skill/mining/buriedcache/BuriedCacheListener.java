package me.mykindos.betterpvp.progression.profession.skill.mining.buriedcache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

@BPvPListener
@Singleton
public class BuriedCacheListener implements Listener {

    @Inject
    private BuriedCache skill;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Chest chest)) return;
        if (!skill.isActiveCache(chest.getBlock())) return;

        UtilServer.runTaskLater(skill.getProgression(), () -> skill.expireChest(chest.getBlock()), 1L);
    }
}
