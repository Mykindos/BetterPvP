package me.mykindos.betterpvp.clans.scoreboards;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@BPvPListener
public class ClansSidebarListener implements Listener {

    @Inject
    @Config(path = "server.sidebar.title", defaultValue = "Mineplex Clans")
    private String sidebarTitle;

    @Inject
    @Config(path = "server.isHub", defaultValue = "false")
    private boolean isHub;

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    public ClansSidebarListener(ClanManager clanManager, ClientManager clientManager) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void updateSideBar(ScoreboardUpdateEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();

        if (isHub || !event.getPlayer().isOnline()) {
            return;
        }

        final Client client = clientManager.search().online(event.getPlayer());
        if (!client.isLoaded()) {
            return;
        }
        Optional<Boolean> sideBarEnabled = client.getProperty(ClientProperty.SIDEBAR_ENABLED);
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
            Component title = Component.text("  " + sidebarTitle + "  ", NamedTextColor.GOLD, TextDecoration.BOLD);
            sidebarObjective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, title);
            sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Set<String> entries = new HashSet<>(scoreboard.getEntries());

        for (String s : entries) {
            if (s == null) continue;
            if (!s.contains("\u00A7")) {
                continue;
            }
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

            if (!clan.getTerritory().isEmpty()) {
                sidebarObjective.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Clan Energy").setScore(12);
                sidebarObjective.getScore(ChatColor.GREEN.toString() + clan.getEnergyTimeRemaining()).setScore(11);
                sidebarObjective.getScore(ChatColor.BLUE + "").setScore(10);
            }
        }


        Optional<Integer> coinsOptional = client.getGamer().getProperty(GamerProperty.BALANCE);
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
    }
}
