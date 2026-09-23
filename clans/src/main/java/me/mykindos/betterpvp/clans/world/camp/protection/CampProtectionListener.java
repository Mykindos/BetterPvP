package me.mykindos.betterpvp.clans.world.camp.protection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import me.mykindos.betterpvp.core.world.zone.ZoneInteractEvent;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Everything that could change a camp's land without a player breaking or placing a block, which the camp zones cannot
 * see: explosions, fire, decay, mobs, trampling and bone meal. Also tells a player why the zones turned them away.
 */
@BPvPListener
@Singleton
public class CampProtectionListener implements Listener {

    private final Camps camps;
    private final ClientManager clientManager;
    private final ZoneManager zoneManager;

    @Inject
    public CampProtectionListener(@NotNull Camps camps, @NotNull ClientManager clientManager,
                                  @NotNull ZoneManager zoneManager) {
        this.camps = camps;
        this.clientManager = clientManager;
        this.zoneManager = zoneManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDenied(ZoneInteractEvent event) {
        if (!event.isInform() || event.getResult() != Event.Result.DENY || !event.getZone().hasTag(CampGrounds.TAG)) {
            return;
        }
        final String key = event.getInteraction() == ZoneInteraction.INTERACT
                ? "clans.camp.protection.private"
                : "clans.camp.protection.land";
        UtilMessage.message(event.getPlayer(), "clans.prefix.camp", key);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isCamp(event.getEntity().getWorld())) {
            event.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (isCamp(event.getBlock().getWorld())) {
            event.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isCamp(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL && isCamp(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE && isCamp(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent event) {
        if (isCamp(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock) && !(event.getEntity() instanceof Player)
                && isCamp(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.FARMLAND && isCamp(event.getPlayer().getWorld())) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        final Player player = event.getPlayer();
        if (!isCamp(event.getBlock().getWorld()) || player != null && isStaff(player)) {
            return;
        }
        final boolean inFarm = zoneManager.hasTagAt(event.getBlock().getLocation(), CampGrounds.FARM);
        if (!inFarm || player == null || !camps.isMember(player, player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!isCamp(event.getEntity().getWorld())) {
            return;
        }
        if (event instanceof HangingBreakByEntityEvent byEntity && byEntity.getRemover() instanceof Player player
                && isStaff(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (isCamp(event.getPlayer().getWorld()) && !isStaff(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean isCamp(@NotNull World world) {
        return camps.clanOf(world).isPresent();
    }

    private boolean isStaff(@NotNull Player player) {
        return clientManager.search().online(player).isAdministrating();
    }
}
