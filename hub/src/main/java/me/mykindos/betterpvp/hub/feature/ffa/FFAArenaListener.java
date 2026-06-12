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
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.world.zone.PlayerEnterZoneEvent;
import me.mykindos.betterpvp.core.world.zone.PlayerExitZoneEvent;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.hub.feature.HubInventoryService;
import me.mykindos.betterpvp.hub.feature.zone.HubZones;
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

    private final ZoneManager zoneManager;
    private final ClientManager clientManager;
    private final FFARegionService ffaRegionService;
    private final HubInventoryService inventoryService;
    private final RolePlaceholderVisibility rolePlaceholderVisibility;
    private final CombatFeaturesService combatFeaturesService;
    private final ArmorProtocol armorProtocol;
    private final Set<UUID> ffaRespawns = ConcurrentHashMap.newKeySet();

    @Inject
    public FFAArenaListener(ZoneManager zoneManager, ClientManager clientManager, FFARegionService ffaRegionService,
                            HubInventoryService inventoryService, RolePlaceholderVisibility rolePlaceholderVisibility,
                            CombatFeaturesService combatFeaturesService, ArmorProtocol armorProtocol, TeleportRules teleportRules) {
        this.zoneManager = zoneManager;
        this.clientManager = clientManager;
        this.ffaRegionService = ffaRegionService;
        this.inventoryService = inventoryService;
        this.rolePlaceholderVisibility = rolePlaceholderVisibility;
        this.combatFeaturesService = combatFeaturesService;
        this.armorProtocol = armorProtocol;
        teleportRules.putRule("hub-ffa", (entity, destination) -> isInFFA(zoneManager, ffaRegionService, entity, destination));
    }

    private static boolean isInFFA(ZoneManager zoneManager, FFARegionService ffaRegionService, LivingEntity entity, Location destination) {
        return entity instanceof Player player
                && zoneManager.isInZone(player, HubZones.FFA)
                && ffaRegionService.contains(destination);
    }

    @EventHandler
    public void onEnterFfa(PlayerEnterZoneEvent event) {
        final boolean ffa = event.getZone().is(HubZones.FFA);
        this.combatFeaturesService.setActive(event.getPlayer(), ffa);
        this.rolePlaceholderVisibility.setVisible(event.getPlayer(), ffa);
        armorProtocol.broadcast(event.getPlayer(), true);
        if (!ffa) {
            return;
        }

        inventoryService.applyFfaLoadout(event.getPlayer());
        sendSubtitle(event.getPlayer(), true);
    }

    @EventHandler
    public void onExitFfa(PlayerExitZoneEvent event) {
        if (!event.getZone().is(HubZones.FFA)) {
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
        if (!zoneManager.isInZone(player, HubZones.FFA) || ffaRegionService.contains(event.getTo())) {
            return;
        }

        if (isCombatLocked(player)) {
            event.setTo(event.getFrom());
            UtilMessage.message(player, "core.prefix.ffa", "hub.ffa.combat-locked");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleportOutOfFfa(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (!zoneManager.isInZone(player, HubZones.FFA) || ffaRegionService.contains(event.getTo())) {
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
            UtilMessage.message(player, "core.prefix.ffa", "hub.ffa.combat-locked");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathMessage(CustomDeathMessageEvent event) {
        if (!zoneManager.isInZone(event.getReceiver(), HubZones.FFA)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (zoneManager.isInZone(event.getPlayer(), HubZones.FFA)) {
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

        final Component action = Translations.component(entered ? "hub.ffa.action.entered" : "hub.ffa.action.exited")
                .color(NamedTextColor.GRAY);
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(50, TitleComponent.subtitle(0.15,
                1.2,
                0.2,
                false,
                gmr -> Translations.component("hub.ffa.subtitle", action, Component.text("FFA", NamedTextColor.YELLOW))));
    }
}
