package me.mykindos.betterpvp.clans.scoreboards;

import com.google.inject.Inject;
import java.util.Optional;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

@BPvPListener
public class ClansNameScoreboardListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClansNameScoreboardListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onScoreboardUpdate(ScoreboardUpdateEvent event) {

        setup(event.getPlayer());

    }

    public void setup(Player player) {

        player.getScoreboard().getTeams().forEach(Team::unregister);
        Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

        clanManager.getObjects().values().forEach(clan -> {
            if (!clan.isOnline()) return;
            addClan(player, playerClan, clan);

        });

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(onlinePlayer);
            if (clanOptional.isEmpty()) {
                addNone(player, onlinePlayer.getName());
            }
        }
    }


    private void addNone(Player player, String name) {

        Scoreboard scoreboard = player.getScoreboard();
        Team noTeam = scoreboard.getTeam("None");
        if (noTeam == null) {
            noTeam = scoreboard.registerNewTeam("None");
            noTeam.color(NamedTextColor.YELLOW);
            noTeam.prefix(Component.text("", NamedTextColor.YELLOW));
        }
        if (!noTeam.hasEntry(name)) {
            noTeam.addEntry(name);
        }

    }


    public void addClan(Player player, Clan playerClan, Clan targetClan) {

        Scoreboard scoreboard = player.getScoreboard();
        if (!isTeam(scoreboard, targetClan.getName())) {
            Team team = scoreboard.registerNewTeam(targetClan.getName());

            setPrefix(team, playerClan, targetClan);
            addMembersToTeam(team, targetClan);

        }

    }

    private void addMembersToTeam(Team team, Clan clan) {
        for(Player player : clan.getMembersAsPlayers()){
            String names = player.getName();
            if (!team.hasEntry(names)) {
                team.addEntry(names);
            }
        }
    }

    public boolean isTeam(Scoreboard scoreboard, String name) {
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }


    private void setPrefix(Team team, Clan playerClan, Clan targetClan) {
        if (playerClan != null && targetClan != null) {

            ClanRelation relation = clanManager.getRelation(playerClan, targetClan);
            String prefix = targetClan.getName();
            if (relation == ClanRelation.ALLY) {
                team.prefix(Component.text(prefix + " ", NamedTextColor.DARK_GREEN));
                team.color(NamedTextColor.GREEN);
                team.suffix(Component.text(""));
            } else if (relation == ClanRelation.ALLY_TRUST) {
                team.prefix(Component.text(prefix + " ", NamedTextColor.DARK_GREEN));
                team.color(NamedTextColor.DARK_GREEN);
                team.suffix(Component.text(""));
            } else if (relation == ClanRelation.ENEMY) {
                team.prefix(Component.text(prefix + " ", NamedTextColor.DARK_RED));
                team.color(NamedTextColor.RED);
                team.suffix(targetClan.getSimpleDominanceString(playerClan));
            } else if (relation == ClanRelation.PILLAGE) {
                team.prefix(Component.text(prefix + " ", NamedTextColor.DARK_PURPLE));
                team.color(NamedTextColor.LIGHT_PURPLE);
                team.suffix(Component.text(""));
            } else if (relation == ClanRelation.SELF) {
                team.prefix(Component.text(prefix + " ", NamedTextColor.DARK_AQUA));
                team.color(NamedTextColor.AQUA);
                team.suffix(Component.text(""));
            } else {
                team.prefix(Component.text(prefix + " ", NamedTextColor.GOLD));
                team.color(NamedTextColor.YELLOW);
                team.suffix(Component.text(""));
            }
        } else {
            if (playerClan == null && targetClan != null) {
                String prefix = targetClan.getName();
                team.prefix(Component.text(prefix + " ", NamedTextColor.GOLD));
            } else {
                team.prefix(Component.text("", NamedTextColor.YELLOW));
            }
            team.color(NamedTextColor.YELLOW);
            team.suffix(Component.text(""));
        }
    }

}
