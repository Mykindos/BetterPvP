package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanCoreTeleportEvent;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanStuckTeleportEvent;
import me.mykindos.betterpvp.core.framework.events.kill.PlayerSuicideEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.world.events.SpawnTeleportEvent;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.PlayerEnterZoneEvent;
import me.mykindos.betterpvp.core.world.zone.PlayerExitZoneEvent;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.Optional;

@BPvPListener
@Singleton
public class ClansMovementListener extends ClanListener {

    private final Clans clans;
    private final ClanManager clanManager;

    @Inject
    public ClansMovementListener(Clans clans, ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onEnterZone(PlayerEnterZoneEvent event) {
        displayZone(event.getPlayer(), event.getZone());
    }

    @EventHandler
    public void onExitZone(PlayerExitZoneEvent event) {
        final Player player = event.getPlayer();
        // If the player moved straight into another zone, the enter event handles the display. Only announce
        // wilderness when they are no longer in any zone.
        if (clanManager.getZoneManager().getZone(player) != null) {
            return;
        }

        final Client client = clientManager.search().online(player);
        final boolean popupSetting = (boolean) client.getProperty(ClientProperty.TERRITORY_POPUPS_ENABLED).orElse(false);
        if (popupSetting) {
            TitleComponent titleComponent = new TitleComponent(0, .75, .25, false,
                    gamer -> Component.text("", NamedTextColor.GRAY),
                    gamer -> Component.text("Wilderness", NamedTextColor.GRAY));
            client.getGamer().getTitleQueue().add(9, titleComponent);
        }

        UtilMessage.message(player, "Territory", Component.text("Wilderness", NamedTextColor.GRAY));
    }

    public void displayZone(Player player, Zone zone) {
        final NamedTextColor color = zoneColor(player, zone);
        final Component name = zone.getDisplayName().applyFallbackStyle(color);

        Clan owner = null;
        Component append = Component.empty();
        if (zone.hasTag(ClanZones.TERRITORY)) {
            owner = clanManager.getClanByLocation(player.getLocation()).orElse(null);
            if (owner != null) {
                final Clan self = clanManager.getClanByPlayer(player).orElse(null);
                final ClanRelation relation = clanManager.getRelation(self, owner);
                if (relation == ClanRelation.ALLY_TRUST) {
                    append = UtilMessage.deserialize(" <gray>(<yellow>Trusted</yellow>)</gray>");
                } else if (relation == ClanRelation.ENEMY && self != null) {
                    append = UtilMessage.deserialize(clanManager.getDominanceString(self, owner));
                }
            }
        }

        if (zone.hasTag(ClanZones.FIELDS)) {
            append = UtilMessage.deserialize("<red><bold>                    Warning! <gray> PvP Hotspot</gray></bold></red>");
        }

        final Client client = clientManager.search().online(player);
        final boolean popupSetting = (boolean) client.getProperty(ClientProperty.TERRITORY_POPUPS_ENABLED).orElse(false);
        if (popupSetting) {
            TitleComponent titleComponent = new TitleComponent(0, .75, .1, true,
                    gamer -> Component.text("", NamedTextColor.GRAY),
                    gamer -> zone.getDisplayName().applyFallbackStyle(color));
            client.getGamer().getTitleQueue().add(9, titleComponent);
        }

        final Component message = name.append(append);
        if (owner != null) {
            final Clan finalOwner = owner;
            UtilServer.runTaskAsync(clans, () ->
                    UtilMessage.simpleMessage(player, "Territory", message, clanManager.getClanTooltip(player, finalOwner)));
        } else {
            UtilMessage.message(player, "Territory", message);
        }
    }

    /**
     * Resolves the colour a zone's name should be shown in, following the relation colour scheme: safe zones use the
     * {@link ClanRelation#SAFE safe} colour, clan territory uses the viewer's relation to the owning clan, and any other
     * zone falls back to a neutral grey.
     */
    private NamedTextColor zoneColor(Player player, Zone zone) {
        if (zone.hasTag(Zones.SAFE)) {
            return ClanRelation.SAFE.getPrimary();
        }
        if (zone.hasTag(ClanZones.TERRITORY)) {
            final Clan self = clanManager.getClanByPlayer(player).orElse(null);
            final Clan owner = clanManager.getClanByLocation(player.getLocation()).orElse(null);
            return clanManager.getRelation(self, owner).getPrimary();
        }
        return NamedTextColor.GRAY;
    }

    @EventHandler
    public void onClanHomeTeleport(ClanCoreTeleportEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

        if (!clanManager.canTeleport(player)) {
            UtilMessage.message(player, "Clans", "You cannot teleport while combat tagged.");
            event.setCancelled(true);
            return;
        }

        String worldName = event.getPlayer().getWorld().getName();
        if (!worldName.equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME) && !worldName.equalsIgnoreCase(BPvPWorld.BOSS_WORLD_NAME)) {
            UtilMessage.message(player, "Clans", "You cannot teleport to your clan home from this world.");
            event.setCancelled(true);
            return;
        }

        if (clanManager.getZoneManager().hasTagAt(player.getLocation(), Zones.SAFE)) {
            event.setDelayInSeconds(0);
        } else {
            clanManager.getClanByLocation(player.getLocation()).ifPresentOrElse(clan -> {
                Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
                if (playerClanOptional.isPresent()) {
                    Clan playerClan = playerClanOptional.get();

                    if (clan.equals(playerClan)) {
                        boolean hasEnemies = false;

                        for (ClanTerritory territory : playerClan.getTerritory()) {
                            Chunk chunk = UtilWorld.stringToChunk(territory.getChunk());
                            if (chunk == null) continue;

                            for (Entity entity : chunk.getEntities()) {
                                if (entity instanceof Player target && !entity.equals(player) && clanManager.canHurt(player, target)) {
                                    hasEnemies = true;
                                    break;
                                }
                            }

                            if (hasEnemies) break;
                        }

                        event.setDelayInSeconds(hasEnemies ? 20 : 0);
                        return;
                    }

                    if (clanManager.getRelation(playerClan, clan) == ClanRelation.ENEMY) {
                        event.setDelayInSeconds(60);
                    } else {
                        event.setDelayInSeconds(30);
                    }

                } else {
                    event.setDelayInSeconds(20);
                }

            }, () -> event.setDelayInSeconds(20));
        }

        if (event.getDelayInSeconds() > 0) {
            UtilMessage.simpleMessage(player, "Clans", "Teleporting to clan core in <alt>%.1f</alt> seconds, don't move!", event.getDelayInSeconds());
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onClanStuckTeleport(ClanStuckTeleportEvent event) {

        Player player = event.getPlayer();

        if (!clanManager.canTeleport(player)) {
            UtilMessage.message(player, "Clans", "You cannot teleport while combat tagged.");
            event.setCancelled(true);
            return;
        }

        Optional<Location> nearestWilderness = clanManager.closestWilderness(player);

        if (nearestWilderness.isEmpty()) {
            UtilMessage.message(player, "Clans", Component.text("No wilderness found to teleport to", NamedTextColor.RED));
            return;
        }

        Optional<Clan> territoryOptional = clanManager.getClanByLocation(player.getLocation());

        if (territoryOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", Component.text("You must be in a claimed territory to use ", NamedTextColor.GRAY)
                    .append(Component.text("/c stuck", NamedTextColor.YELLOW)));
            event.cancel("In wilderness.");
            return;
        }

        event.setDelayInSeconds(30);

        if (event.getDelayInSeconds() > 0) {
            UtilMessage.simpleMessage(player, "Clans", "Teleporting to nearest wilderness in <green>%.1f</green> seconds, don't move!", event.getDelayInSeconds());
        }
    }

    @EventHandler
    public void onSpawnTeleport(SpawnTeleportEvent event) {
        if (event.isCancelled()) return;

        final Client client = clientManager.search().online(event.getPlayer());
        if (client.hasRank(Rank.ADMIN)) {
            return;
        }

        if (clanManager.getClanByLocation(event.getPlayer().getLocation()).isPresent()) {
            UtilMessage.message(event.getPlayer(), "Spawn", "You can only teleport to spawn from the wilderness.");
            event.setCancelled(true);
        } else {
            event.setDelayInSeconds(30);
        }

    }

    @EventHandler
    public void onSuicide(PlayerSuicideEvent event) {
        if (event.isCancelled()) return;

        final Client client = clientManager.search().online(event.getPlayer());
        if (client.hasRank(Rank.ADMIN)) {
            return;
        }

        if (clanManager.getZoneManager().hasTagAt(event.getPlayer().getLocation(), Zones.SAFE)) {
            event.setDelayInSeconds(0);
            return;
        }

        clanManager.getClanByLocation(event.getPlayer().getLocation()).ifPresentOrElse(clan -> {
            Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());
            if (playerClanOptional.isPresent()) {
                if (playerClanOptional.get().equals(clan) && !chunkHasEnemies(event.getPlayer())) {
                    event.setDelayInSeconds(0);
                    return;
                }
            }
            event.setDelayInSeconds(15);
        }, () -> event.setDelayInSeconds(15));
    }

    public boolean chunkHasEnemies(Player player){
        Chunk chunk = player.getLocation().getChunk();
        if (chunk.getEntities() != null) {
            for (Entity entities : chunk.getEntities()) {
                if (entities instanceof Player target) {
                    if (entities.equals(player)) {
                        continue;
                    }

                    if (clanManager.getClanByPlayer(player).isEmpty() || clanManager.getClanByPlayer(target).isEmpty()) {
                        continue;
                    }

                    Clan playerClan = clanManager.getClanByPlayer(player).get();
                    Clan targetClan = clanManager.getClanByPlayer(target).get();

                    if (clanManager.canHurt(player, target) && clanManager.getRelation(playerClan, targetClan) == ClanRelation.ENEMY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
