package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.interaction.event.TreeFellerEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Iterator;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class TreeFellerListener implements Listener {

    private final ZoneManager zoneManager;

    @Inject
    private TreeFellerListener(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFell(TreeFellerEvent event) {
        final Player player = event.getPlayer();

        final Iterator<Block> iterator = event.getBlocks().iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            // inform=false: pre-filtering blocks, so suppress the per-block denial message.
            final Event.Result verdict = zoneManager.queryAccess(player, block.getLocation(), ZoneInteraction.BREAK, block, false);
            if (verdict == Event.Result.DENY) {
                iterator.remove(); // remove the block from the list of blocks to break
            }
        }
    }
}
