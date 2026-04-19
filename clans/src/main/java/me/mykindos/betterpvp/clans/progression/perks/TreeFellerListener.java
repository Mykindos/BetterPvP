package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.interaction.event.TreeFellerEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class TreeFellerListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    private TreeFellerListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFell(TreeFellerEvent event) {
        final Player player = event.getPlayer();

        final Iterator<Block> iterator = event.getBlocks().iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(block.getLocation());
            if (targetBlockLocationClanOptional.isEmpty()) {
                continue; // no clan
            }

            final Clan blockClan = targetBlockLocationClanOptional.get();
            final TerritoryInteractEvent tie = new TerritoryInteractEvent(player,
                    blockClan,
                    block,
                    Event.Result.DEFAULT,
                    TerritoryInteractEvent.InteractionType.BREAK);
            tie.setInform(false); // we don't want to spam them
            tie.callEvent();
            if (tie.getResult() == Event.Result.DENY) {
                iterator.remove(); // remove the block from the list of blocks to break
            }
        }

    }
}
