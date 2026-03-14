package me.mykindos.betterpvp.core.item.listener;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;

@BPvPListener
@Singleton
public class ItemBehaviorListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMerge(ItemMergeEvent event) {
        if (!UtilItem.isMergeable(event.getEntity()) || !UtilItem.isMergeable(event.getTarget())) {
            event.setCancelled(true);
        }
    }

}
