package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.resource.event.ResourceHarvestAttemptEvent;
import me.mykindos.betterpvp.clans.world.resource.event.ResourceHarvestEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneInteractEvent;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes interactions to resource nodes and enforces the shared harvest flow (the generalised replacement for
 * {@code FieldsListener}). Block break/interact arrives via {@link ZoneInteractEvent} (the node's
 * {@link ResourceNodeRule} denies it, exactly as the Fields zone did, so we intercept); fishing arrives via
 * {@link PlayerFishEvent}. Both pass through the profession-level {@link ResourceHarvestAttemptEvent gate} and, on a
 * consumed harvest, fire {@link ResourceHarvestEvent} (XP) and clan experience.
 */
@Singleton
@BPvPListener
public class ResourceNodeManager implements Listener {

    private final ZoneManager zoneManager;
    private final ClientManager clientManager;
    private final Map<Key, ResourceNodeProp> byZoneKey = new ConcurrentHashMap<>();

    @Inject
    public ResourceNodeManager(@NotNull ZoneManager zoneManager, @NotNull ClientManager clientManager) {
        this.zoneManager = zoneManager;
        this.clientManager = clientManager;
    }

    public void register(@NotNull ResourceNodeProp node) {
        byZoneKey.put(node.getZone().getKey(), node);
    }

    public void unregister(@NotNull ResourceNodeProp node) {
        byZoneKey.remove(node.getZone().getKey());
    }

    public void clear() {
        byZoneKey.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onZoneInteract(ZoneInteractEvent event) {
        final ResourceNodeProp node = byZoneKey.get(event.getZone().getKey());
        if (node == null) {
            return;
        }
        final ZoneInteraction interaction = event.getInteraction();
        if (interaction != ZoneInteraction.BREAK && interaction != ZoneInteraction.INTERACT) {
            return;
        }

        // Administrating clients edit the tree in-world, so let the real break through (the zone rule already returns
        // DEFAULT for them) rather than consuming the hit and re-cancelling it. Everyone else cannot break a schematic
        // block at all — the rule cancels it — but the swing still counts toward felling.
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && clientManager.search().online(player).isAdministrating()) {
            return;
        }

        final Block block = event.getBlock();
        // An unbreakable chain terminal (e.g. deepslate) must never break, even when a held dig steps a block into it
        // via setType: no fresh BlockDamageEvent fires for the new stage (so onBlockDamage can't cancel it), and an
        // administrating player's rule verdict is DEFAULT rather than DENY, so the break would otherwise go through.
        // Deny it here so completeBreak's breakBlock is rejected — mirroring onBlockDamage, which gates everyone.
        if (interaction == ZoneInteraction.BREAK && block != null
                && node.getArchetype().isUnbreakable(node, block)) {
            event.setInform(false);
            event.setResult(Event.Result.DENY);
            return;
        }

        if (!gate(player, node)) {
            event.setInform(false);
            return;
        }
        final org.bukkit.Material harvested = block != null ? block.getType() : null;
        if (node.getArchetype().onHarvest(node, player, block, interaction)) {
            event.setInform(false);
            event.setResult(Event.Result.DENY);
            afterHarvest(player, node, block, harvested);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || event.getHook() == null) {
            return;
        }
        final ResourceNodeProp node = resolveAt(event.getHook().getLocation());
        if (node == null) {
            return;
        }
        final Player player = event.getPlayer();
        if (!gate(player, node)) {
            event.setCancelled(true);
            return;
        }
        if (node.getArchetype().onFish(node, player, event)) {
            afterHarvest(player, node, null, null);
        }
    }

    /**
     * Cancels the start-of-mining damage on a block the archetype reports as unbreakable (e.g. a chain's unbreakable
     * terminal stage), so no cracking animation plays. Administrating players bypass it so they can edit nodes in-world.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        final Player player = event.getPlayer();

        // Administrating clients edit the tree in-world, so let the real break through (the zone rule already returns
        // DEFAULT for them) rather than consuming the hit and re-cancelling it. Everyone else cannot break a schematic
        // block at all — the rule cancels it — but the swing still counts toward felling.
        if (player.getGameMode() == GameMode.CREATIVE && clientManager.search().online(player).isAdministrating()) {
            return;
        }

        final ResourceNodeProp node = resolveAt(event.getBlock().getLocation());
        if (node != null && node.getArchetype().isUnbreakable(node, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Fires the gate event and returns whether harvesting may proceed; sends the denial message if it was cancelled.
     */
    private boolean gate(@NotNull Player player, @NotNull ResourceNodeProp node) {
        final ResourceHarvestAttemptEvent attempt =
                new ResourceHarvestAttemptEvent(player, node, node.getDefinition().getProfession(), node.getLevel());
        attempt.callEvent();
        if (attempt.isCancelled()) {
            if (attempt.getDenialMessage() != null) {
                player.sendMessage(attempt.getDenialMessage());
            }
            return false;
        }
        return true;
    }

    private void afterHarvest(@NotNull Player player, @NotNull ResourceNodeProp node, @Nullable Block block,
                              @Nullable org.bukkit.Material harvested) {
        new ResourceHarvestEvent(player, node, node.getDefinition().getProfession(), block, harvested).callEvent();
        UtilServer.callEvent(new ClanAddExperienceEvent(player, 0.1));
    }

    private @Nullable ResourceNodeProp resolveAt(@NotNull org.bukkit.Location location) {
        for (Zone zone : zoneManager.getZonesAt(location)) {
            final ResourceNodeProp node = byZoneKey.get(zone.getKey());
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
