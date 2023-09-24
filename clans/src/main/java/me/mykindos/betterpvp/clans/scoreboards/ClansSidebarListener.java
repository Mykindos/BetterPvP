package me.mykindos.betterpvp.clans.scoreboards;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Optional;

@BPvPListener
public class ClansSidebarListener implements Listener {

    @Inject
    @Config(path = "server.sidebar.title", defaultValue = "BetterPvP")
    private String sidebarTitle;

    @Inject
    @Config(path = "server.isHub", defaultValue = "false")
    private boolean isHub;

    private final ClanManager clanManager;
    private final GamerManager gamerManager;

    @Inject
    public ClansSidebarListener(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void updateSideBar(ScoreboardUpdateEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();

        if (isHub) {
            return;
        }

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if (gamerOptional.isPresent()) {
            Gamer gamer = gamerOptional.get();

            Optional<Boolean> sideBarEnabled = gamer.getProperty(GamerProperty.SIDEBAR_ENABLED);
            if (sideBarEnabled.isPresent()) {
                if (!sideBarEnabled.get()) {
                    player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                    Objective side = scoreboard.getObjective("sidebar");
                    if (side != null) {
                        side.unregister();
                    }

                    return;
                }
            }

            Objective sidebarObjective = scoreboard.getObjective("sidebar");
            if (sidebarObjective == null) {
                Component title = Component.text(ChatColor.GOLD.toString() + ChatColor.BOLD + "  " + sidebarTitle + "  ");
                sidebarObjective = scoreboard.registerNewObjective("sidebar", "dummy", title);
                sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }

            for (String s : scoreboard.getEntries()) {
                scoreboard.resetScores(s);
            }

            Clan clan = null;

            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            if (clanOptional.isPresent()) {
                clan = clanOptional.get();

                sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Clan").setScore(15);
                String name = clan.getName();

                sidebarObjective.getScore(ChatColor.AQUA.toString() + name + ChatColor.RESET).setScore(14);
                sidebarObjective.getScore(ChatColor.RED + "").setScore(13);

                if (clan.getTerritory().size() > 0) {
                    sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Clan Energy").setScore(12);
                    sidebarObjective.getScore(ChatColor.GREEN.toString() + clan.getEnergyTimeRemaining()).setScore(11);
                    sidebarObjective.getScore(ChatColor.BLUE + "").setScore(10);
                }
            }


            Optional<Integer> coinsOptional = gamer.getProperty(GamerProperty.BALANCE);
            if (coinsOptional.isPresent()) {
                sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Coins").setScore(9);
                sidebarObjective.getScore(ChatColor.GOLD.toString() + UtilFormat.formatNumber(coinsOptional.get())).setScore(8);
            }


            sidebarObjective.getScore(ChatColor.YELLOW + "").setScore(7);
            sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Territory").setScore(6);
            Optional<Clan> standingOptional = clanManager.getClanByLocation(player.getLocation());
            if (standingOptional.isPresent()) {
                Clan standing = standingOptional.get();
                sidebarObjective.getScore(clanManager.getRelation(clan, standing).getPrimaryAsChatColor().toString()
                        + standing.getName()).setScore(5);
            } else {
                sidebarObjective.getScore(ChatColor.GRAY.toString() + "Wilderness").setScore(5);
            }

            // TODO world event

        }
    }
}
