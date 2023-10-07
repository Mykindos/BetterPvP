package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanHomeTeleportEvent;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanStuckTeleportEvent;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.events.SpawnTeleportEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

@BPvPListener
public class ClansMovementListener extends ClanListener {

    private final Clans clans;

    @Inject
    public ClansMovementListener(Clans clans, ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        this.clans = clans;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {

        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {


            UtilServer.runTaskAsync(clans, () -> {
                Optional<Clan> clanToOptional = clanManager.getClanByLocation(e.getTo());
                Optional<Clan> clanFromOption = clanManager.getClanByLocation(e.getFrom());


                if (clanToOptional.isEmpty() && clanFromOption.isEmpty()) {
                    return;
                }


                if (clanFromOption.isEmpty() || clanToOptional.isEmpty()
                        || !clanFromOption.equals(clanToOptional)) {
                    displayOwner(e.getPlayer(), clanToOptional.orElse(null));

                    UtilServer.runTask(clans, () -> UtilServer.callEvent(new ScoreboardUpdateEvent(e.getPlayer())));

                }
            });

        }

    }

    public void displayOwner(Player player, Clan locationClan) {

        Component component = Component.empty().color(NamedTextColor.YELLOW).append(Component.text("Wilderness"));

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
                    append = UtilMessage.deserialize(clan.getDominanceString(locationClan));
                }
            }
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

        clanManager.getClanByLocation(event.getPlayer().getLocation()).ifPresentOrElse(clan -> {
            if (clan.isAdmin()) {
                if (clan.isSafe() && clan.getName().contains("Spawn") && event.getPlayer().getLocation().getY() > 110) {
                    return;
                }
            }

            UtilMessage.message(event.getPlayer(), "Clans", "You can only teleport to your clan home from spawn or the wilderness.");
            event.setCancelled(true);
        }, () -> {
            event.setDelayInSeconds(30);
        });

    }

    @EventHandler
    public void onClanStuckTeleport(ClanStuckTeleportEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

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
            event.setDelayInSeconds(3 * 5);
        } else {
            event.setDelayInSeconds(2 * 5);
        }
    }

    @EventHandler
    public void onSpawnTeleport(SpawnTeleportEvent event) {
        if (event.isCancelled()) return;

        gamerManager.getObject(event.getPlayer().getUniqueId()).ifPresent(gamer -> {
            if (gamer.getClient().hasRank(Rank.ADMIN)) {
                return;
            }

            if (clanManager.getClanByLocation(event.getPlayer().getLocation()).isPresent()) {
                UtilMessage.message(event.getPlayer(), "Spawn", "You can only teleport to spawn from the wilderness.");
                event.setCancelled(true);
            } else {
                event.setDelayInSeconds(30);
            }

        });

    }
}
