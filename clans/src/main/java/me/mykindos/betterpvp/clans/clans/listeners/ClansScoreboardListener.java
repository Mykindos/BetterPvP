package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Criterias;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Optional;

@BPvPListener
public class ClansScoreboardListener implements Listener {

    @Inject
    @Config(path = "server.sidebar.title", defaultValue = "BetterPvP")
    private String sidebarTitle;

    private final ClanManager clanManager;
    private final GamerManager gamerManager;

    @Inject
    public ClansScoreboardListener(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {
        Bukkit.getPluginManager().callEvent(new ScoreboardUpdateEvent(event.getPlayer()));
    }

    @EventHandler
    public void updateHealthBarScoreboard(ScoreboardUpdateEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();

        Objective healthObjective = scoreboard.getObjective("showhealth");
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective("showhealth", "health", Component.text(ChatColor.RED + "\u2764"));

            healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
    }

    @EventHandler
    public void updateSideBar(ScoreboardUpdateEvent event){
        Player player = event.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();
        // TODO check if sidebar enabled
        // TODO check if in hub

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
        if(clanOptional.isPresent()){
            clan = clanOptional.get();

            sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Clan").setScore(15);
            String name = clan.getName();

            sidebarObjective.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD + name + "").setScore(14);
            sidebarObjective.getScore(ChatColor.RED + "").setScore(13);

            if (clan.getTerritory().size() > 0) {
                sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Clan Energy").setScore(12);
                sidebarObjective.getScore(ChatColor.GREEN.toString() + ChatColor.BOLD + clan.getEnergyTimeRemaining()).setScore(11);
                sidebarObjective.getScore(ChatColor.BLUE + "").setScore(10);
            }
        }

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if(gamerOptional.isPresent()) {
            Gamer gamer = gamerOptional.get();

            Optional<Integer> coinsOptional = gamer.getProperty(GamerProperty.COINS);
            if(coinsOptional.isPresent()){
                sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Coins").setScore(9);
                sidebarObjective.getScore(ChatColor.GOLD.toString() + ChatColor.BOLD + UtilFormat.formatNumber(coinsOptional.get())).setScore(8);
            }

        }


        sidebarObjective.getScore(ChatColor.YELLOW + "").setScore(7);
        sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Territory").setScore(6);
        Optional<Clan> standingOptional = clanManager.getClanByLocation(player.getLocation());
        if (standingOptional.isPresent()) {
            Clan standing = standingOptional.get();
            sidebarObjective.getScore(clanManager.getRelation(clan, standing).getPrimary(true) + standing.getName()).setScore(5);
        } else {
            sidebarObjective.getScore(ChatColor.GREEN.toString() + ChatColor.BOLD + "Wilderness").setScore(5);
        }

        // TODO world event

    }
}
