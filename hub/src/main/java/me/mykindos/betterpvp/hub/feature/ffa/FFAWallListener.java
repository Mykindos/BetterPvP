package me.mykindos.betterpvp.hub.feature.ffa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.feature.zone.Zone;
import me.mykindos.betterpvp.hub.feature.zone.ZoneService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.BlockVector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@BPvPListener
@Singleton
public class FFAWallListener implements Listener {

    private static final double WALL_RADIUS = 10.0;

    private final ZoneService zoneService;
    private final ClientManager clientManager;
    private final FFARegionService ffaRegionService;
    private final Map<UUID, Set<BlockVector>> rendered = new HashMap<>();

    @Inject
    public FFAWallListener(ZoneService zoneService, ClientManager clientManager, FFARegionService ffaRegionService) {
        this.zoneService = zoneService;
        this.clientManager = clientManager;
        this.ffaRegionService = ffaRegionService;
    }

    @UpdateEvent(delay = 100)
    public void updateWalls() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (zoneService.getZone(player) != Zone.FFA || !clientManager.search().online(player).getGamer().isInCombat()) {
                clear(player);
                continue;
            }

            final Set<BlockVector> desired = ffaRegionService.getNearbyWallBlocks(player.getLocation(), WALL_RADIUS);
            final Set<BlockVector> current = rendered.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());

            final Set<BlockVector> toRemove = new HashSet<>(current);
            toRemove.removeAll(desired);

            final Set<BlockVector> toAdd = new HashSet<>(desired);
            toAdd.removeAll(current);

            show(player, toAdd);
            restore(player, toRemove);

            current.clear();
            current.addAll(desired);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        rendered.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteractWall(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Player player = event.getPlayer();
        final BlockVector vector = event.getClickedBlock().getLocation().toVector().toBlockVector();
        if (!isRendered(player, vector)) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        player.sendBlockChange(ffaRegionService.toLocation(vector), ffaRegionService.getWallData());
    }

    private void clear(Player player) {
        final Set<BlockVector> current = rendered.remove(player.getUniqueId());
        if (current == null) {
            return;
        }

        restore(player, current);
    }

    private void show(Player player, Set<BlockVector> blocks) {
        if (blocks.isEmpty()) {
            return;
        }

        final Map<Location, BlockData> changes = new HashMap<>();
        for (BlockVector block : blocks) {
            changes.put(ffaRegionService.toLocation(block), ffaRegionService.getWallData());
        }
        player.sendMultiBlockChange(changes);
    }

    private void restore(Player player, Set<BlockVector> blocks) {
        if (blocks.isEmpty()) {
            return;
        }

        final Map<Location, BlockData> changes = new HashMap<>();
        for (BlockVector block : blocks) {
            final Location location = ffaRegionService.toLocation(block);
            changes.put(location, location.getBlock().getBlockData());
        }
        player.sendMultiBlockChange(changes);
    }

    private boolean isRendered(Player player, BlockVector block) {
        final Set<BlockVector> current = rendered.get(player.getUniqueId());
        return current != null && current.contains(block);
    }
}
