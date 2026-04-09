package me.mykindos.betterpvp.hub.feature.ffa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RolePlaceholderVisibility;
import me.mykindos.betterpvp.champions.champions.roles.packet.ArmorProtocol;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathMessageEvent;
import me.mykindos.betterpvp.core.framework.TeleportRules;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.hub.feature.HubInventoryService;
import me.mykindos.betterpvp.hub.feature.zone.PlayerEnterZoneEvent;
import me.mykindos.betterpvp.hub.feature.zone.PlayerExitZoneEvent;
import me.mykindos.betterpvp.hub.feature.zone.Zone;
import me.mykindos.betterpvp.hub.feature.zone.ZoneService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@BPvPListener
@Singleton
public class FFAArenaListener implements Listener {

    private final ZoneService zoneService;
    private final ClientManager clientManager;
    private final FFARegionService ffaRegionService;
    private final HubInventoryService inventoryService;
    private final RolePlaceholderVisibility rolePlaceholderVisibility;
    private final CombatFeaturesService combatFeaturesService;
    private final ArmorProtocol armorProtocol;
    private final Set<UUID> ffaRespawns = ConcurrentHashMap.newKeySet();

    @Inject
    public FFAArenaListener(ZoneService zoneService, ClientManager clientManager, FFARegionService ffaRegionService,
                            HubInventoryService inventoryService, RolePlaceholderVisibility rolePlaceholderVisibility,
                            CombatFeaturesService combatFeaturesService, ArmorProtocol armorProtocol, TeleportRules teleportRules) {
        this.zoneService = zoneService;
        this.clientManager = clientManager;
        this.ffaRegionService = ffaRegionService;
        this.inventoryService = inventoryService;
        this.rolePlaceholderVisibility = rolePlaceholderVisibility;
        this.combatFeaturesService = combatFeaturesService;
        this.armorProtocol = armorProtocol;
        teleportRules.putRule("hub-ffa", (entity, destination) -> isInFFA(zoneService, ffaRegionService, entity, destination));
    }

    private static boolean isInFFA(ZoneService zoneService, FFARegionService ffaRegionService, LivingEntity entity, Location destination) {
        return entity instanceof Player player
                && zoneService.getZone(player) == Zone.FFA
                && ffaRegionService.contains(destination);
    }

    @EventHandler
    public void onEnterFfa(PlayerEnterZoneEvent event) {
        this.combatFeaturesService.setActive(event.getPlayer(), event.getZone() == Zone.FFA);
        this.rolePlaceholderVisibility.setVisible(event.getPlayer(), event.getZone() == Zone.FFA);
        armorProtocol.broadcast(event.getPlayer(), true);
        if (event.getZone() != Zone.FFA) {
            return;
        }

        inventoryService.applyFfaLoadout(event.getPlayer());
        sendSubtitle(event.getPlayer(), true);
    }

    @EventHandler
    public void onExitFfa(PlayerExitZoneEvent event) {
        if (event.getZone() != Zone.FFA) {
            return;
        }

        this.combatFeaturesService.setActive(event.getPlayer(), false);
        this.rolePlaceholderVisibility.setVisible(event.getPlayer(), false);
        armorProtocol.broadcast(event.getPlayer(), true);
        inventoryService.applyHubHotbar(event.getPlayer());
        sendSubtitle(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMoveOutOfFfa(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final Player player = event.getPlayer();
        if (zoneService.getZone(player) != Zone.FFA || ffaRegionService.contains(event.getTo())) {
            return;
        }

        if (isCombatLocked(player)) {
            event.setTo(event.getFrom());
            UtilMessage.message(player, "FFA", "You cannot leave the arena while in combat.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleportOutOfFfa(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (zoneService.getZone(player) != Zone.FFA || ffaRegionService.contains(event.getTo())) {
            return;
        }

        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
                || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        if (isCombatLocked(player)) {
            event.setCancelled(true);
            UtilMessage.message(player, "FFA", "You cannot leave the arena while in combat.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathMessage(CustomDeathMessageEvent event) {
        if (zoneService.getZone(event.getReceiver()) != Zone.FFA) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (zoneService.getZone(event.getPlayer()) == Zone.FFA) {
            ffaRespawns.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!ffaRespawns.remove(event.getPlayer().getUniqueId())) {
            return;
        }

        event.setRespawnLocation(ffaRegionService.getSpawnpoint());
        inventoryService.applyFfaLoadout(event.getPlayer());
    }

    private boolean isCombatLocked(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        final Client client = clientManager.search().online(player);
        return client.getGamer().isInCombat() && !client.isAdministrating();
    }

    private void sendSubtitle(Player player, boolean entered) {
        new SoundEffect(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, entered ? 1.7f: 1.3f).play(player);

        final String action = entered ? "Entered" : "Exited";
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(50, TitleComponent.subtitle(0.15,
                1.2,
                0.2,
                false,
                gmr -> UtilMessage.deserialize("<gray>" + action + " <yellow>FFA")));
    }
}
