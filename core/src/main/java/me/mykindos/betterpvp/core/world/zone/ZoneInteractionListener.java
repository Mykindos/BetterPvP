package me.mykindos.betterpvp.core.world.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * The single place raw Bukkit world interactions become zone interactions. Hooks block break/place/interact, resolves
 * the zone at the location, and runs it through {@link ZoneManager#queryAccess} (the zone's {@link ZoneRule}s plus the
 * {@link ZoneInteractEvent} bus) — cancelling the Bukkit event when the verdict is {@link Event.Result#DENY}.
 * <p>
 * Consuming modules never fire zone events themselves; they attach rules to zones or handle {@link ZoneInteractEvent}.
 * Runs at {@link EventPriority#LOW} so a zone denial lands before later protection handlers (which early-return on an
 * already-cancelled event). With no rules and no {@code ZoneInteractEvent} handlers, this is a no-op.
 */
@BPvPListener
@Singleton
public class ZoneInteractionListener implements Listener {

    private final ZoneManager zoneManager;

    @Inject
    private ZoneInteractionListener(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onBreak(BlockBreakEvent event) {
        if (denied(event.getPlayer(), event.getBlock().getLocation(), ZoneInteraction.BREAK, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onPlace(BlockPlaceEvent event) {
        if (denied(event.getPlayer(), event.getBlock().getLocation(), ZoneInteraction.PLACE, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    void onInteract(PlayerInteractEvent event) {
        // Only deliberate main-hand block clicks count as a zone interaction (not off-hand duplicates or physical steps).
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        if (denied(event.getPlayer(), block.getLocation(), ZoneInteraction.INTERACT, block)) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    private boolean denied(Player player, Location location, ZoneInteraction interaction, Block block) {
        if (!zoneManager.isActive()) {
            return false;
        }
        return zoneManager.queryAccess(player, location, interaction, block) == Event.Result.DENY;
    }
}
