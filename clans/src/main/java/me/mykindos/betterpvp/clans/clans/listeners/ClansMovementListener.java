package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Optional;

@BPvPListener
public class ClansMovementListener extends ClanListener{

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
                        ||  !clanFromOption.equals(clanToOptional)) {
                    displayOwner(e.getPlayer(), clanToOptional.orElse(null));

                    UtilServer.runTask(clans, () -> UtilServer.callEvent(new ScoreboardUpdateEvent(e.getPlayer())));

                }
            });

        }

    }

    public void displayOwner(Player player, Clan locationClan) {

        String ownerString = ChatColor.YELLOW + "Wilderness";

        Clan clan = null;
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if(clanOptional.isPresent()){
            clan = clanOptional.get();
        }

        String append = "";

        if (locationClan != null) {
            ClanRelation relation = clanManager.getRelation(clan, locationClan);
            ownerString = relation.getPrimaryAsChatColor() + locationClan.getName();

            if (locationClan.isAdmin()) {
                if (locationClan.isSafe()) {
                    ownerString = ChatColor.WHITE + locationClan.getName();
                    append = ChatColor.WHITE + "(" + ChatColor.AQUA + "Safe" + ChatColor.WHITE + ")";
                }
            } else if (relation == ClanRelation.ALLY_TRUST) {
                append = ChatColor.GRAY + "(" + ChatColor.YELLOW + "Trusted" + ChatColor.GRAY + ")";

            } else if (relation == ClanRelation.ENEMY) {
                if (clan != null) {
                    append = clan.getDominanceString(locationClan);
                }

            }
        }

        if (locationClan != null) {
            if (locationClan.getName().equals("Fields") || locationClan.getName().equals("Lake")) {
                append = ChatColor.RED.toString() + ChatColor.BOLD + "                    Warning! "
                        + ChatColor.GRAY.toString() + ChatColor.BOLD + "PvP Hotspot";
            }

            Component textComponent = Component.text().build();
            List<String> tooltipList = clanManager.getClanTooltip(player, locationClan);
            for(String text : tooltipList) {
                textComponent = textComponent.append(Component.text(text + "\n"));
            }
            UtilMessage.message(player, "Territory", Component.text(ownerString + append).hoverEvent(HoverEvent.showText(textComponent)));
            //
            //new FancyMessage(ChatColor.BLUE + "Territory> " + ownerString + " " + append).tooltip(ClanUtilities.getClanTooltip(p, locationClan)).send(p);
        } else {
            UtilMessage.message(player, "Territory", ownerString + " " + append);
        }


    }
}
