package me.mykindos.betterpvp.champions.listeners;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

@Singleton
@BPvPListener
public class DeathListener implements Listener {

    // Slow items on death because fuck 1.20
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathExplode(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return;
        }

        final ArrayList<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();


        final Player player = event.getPlayer();
        for (ItemStack drop : drops) {
            var item = player.getWorld().dropItemNaturally(player.getLocation(), drop);
          //  item.setOwner(player.getUniqueId());
        }
    }

}
