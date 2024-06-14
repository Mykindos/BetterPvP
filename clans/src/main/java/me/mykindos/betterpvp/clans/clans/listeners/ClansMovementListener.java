package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanHomeTeleportEvent;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanStuckTeleportEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.world.events.SpawnTeleportEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

@BPvPListener
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
    public void onPlayerMoveEvent(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {


            UtilServer.runTaskAsync(clans, () -> {
                Optional<Clan> clanToOptional = clanManager.getClanByLocation(event.getTo());
                Optional<Clan> clanFromOption = clanManager.getClanByLocation(event.getFrom());


                if (clanToOptional.isEmpty() && clanFromOption.isEmpty()) {
                    return;
                }


                if (clanFromOption.isEmpty() || clanToOptional.isEmpty()
                        || !clanFromOption.equals(clanToOptional)) {
                    displayOwner(event.getPlayer(), clanToOptional.orElse(null));
                }
            });

        }

    }

    public void displayOwner(Player player, Clan locationClan) {

        Component component = Component.empty();

        Clan clan = null;
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isPresent()) {
            clan = clanOptional.get();
        }

        Component append = Component.empty();

        if (locationClan != null) {
            ClanRelation relation = clanManager.getRelation(clan, locationClan);
            component = Component.text(locationClan.getName()).color(relation.getPrimary());

            if (locationClan.isAdmin()) {
                if (locationClan.isSafe()) {
                    component = Component.text(locationClan.getName(), NamedTextColor.WHITE);
                    append = UtilMessage.deserialize(" <white>(<aqua>Safe</aqua>)</white>");
                }
            } else if (relation == ClanRelation.ALLY_TRUST) {
                append = UtilMessage.deserialize(" <gray>(<yellow>Trusted</yellow>)</gray>");
            } else if (relation == ClanRelation.ENEMY) {
                if (clan != null) {
                    append = UtilMessage.deserialize(clanManager.getDominanceString(clan, locationClan));
                }
            }
        } else {
            append = Component.text("Wilderness", NamedTextColor.GRAY);
        }

        if (locationClan != null) {
            if (locationClan.getName().equalsIgnoreCase("Fields") || locationClan.getName().equalsIgnoreCase("Lake")) {
                append = UtilMessage.deserialize("<red><bold>                    Warning! <gray> PvP Hotspot</gray></bold></red>");
            }

            UtilMessage.simpleMessage(player, "Territory", component.append(append), clanManager.getClanTooltip(player, locationClan));
        } else {
            UtilMessage.message(player, "Territory", component.append(append));
        }

    }

    @EventHandler
    public void onClanHomeTeleport(ClanHomeTeleportEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

        if (!clanManager.canTeleport(player)) {
            UtilMessage.message(player, "Clans", "You cannot teleport while combat tagged.");
            event.setCancelled(true);
            return;
        }

        clanManager.getClanByLocation(player.getLocation()).ifPresentOrElse(clan -> {
            if (clan.isAdmin() && clan.isSafe()) {
                if (clan.getName().toLowerCase().contains("spawn")) {
                    event.setDelayInSeconds(0);
                } else {
                    event.setDelayInSeconds(20);
                }
                return;
            }

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

                    if (hasEnemies) {
                        event.setDelayInSeconds(20);
                    } else {
                        event.setDelayInSeconds(0);
                    }
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

        }, () -> {
            event.setDelayInSeconds(20);
        });

        if (event.getDelayInSeconds() > 0) {
            UtilMessage.simpleMessage(player, "Clans", "Teleporting to clan home in <green>%.1f</green> seconds, don't move!", event.getDelayInSeconds());
        }
    }


    @EventHandler
    public void onClanStuckTeleport(ClanStuckTeleportEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

        if (!clanManager.canTeleport(player)) {
            UtilMessage.message(player, "Clans", "You cannot teleport while combat tagged.");
            event.setCancelled(true);
            return;
        }

        Location nearestWilderness = clanManager.closestWilderness(player);

        if (nearestWilderness == null) {
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

        ClanRelation relation = clanManager.getRelation(clanManager.getClanByPlayer(player).orElse(null), territoryOptional.get());

        if (relation == ClanRelation.ENEMY) {
            event.setDelayInSeconds(120);
        } else {
            event.setDelayInSeconds(60);
        }
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
}
