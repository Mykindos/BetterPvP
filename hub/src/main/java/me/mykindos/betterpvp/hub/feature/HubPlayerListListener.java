package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.events.ClientRankUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.hub.Hub;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

@BPvPListener
@Singleton
public class HubPlayerListListener implements Listener {

    private static final String TEAM_PREFIX = "hubtab-";

    private final Hub hub;
    private final ClientManager clientManager;

    @Inject
    public HubPlayerListListener(Hub hub, ClientManager clientManager) {
        this.hub = hub;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(ClientJoinEvent event) {
        UtilServer.runTaskLater(hub, this::refreshAllPlayers, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(ClientQuitEvent event) {
        final String playerName = event.getPlayer().getName();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            removeManagedEntry(onlinePlayer.getScoreboard(), playerName);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRankUpdate(ClientRankUpdateEvent event) {
        if (!event.getClient().isLoaded()) {
            return;
        }

        UtilServer.runTaskLater(hub, () -> refreshPlayer(event.getClient()), 1L);
    }

    private void refreshAllPlayers() {
//        for (Client client : clientManager.getOnline()) {
//            refreshPlayer(client);
//        }
    }

    public void refreshPlayer(Client client) {
        final Player player = client.getGamer().getPlayer();
        if (player == null) {
            return;
        }

        player.playerListName(buildPlayerListName(client));

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applyTeam(viewer.getScoreboard(), player, client.getRank());
        }
    }

    private Component buildPlayerListName(Client client) {
        return Component.join(
                JoinConfiguration.noSeparators(),
                client.getRank().getTag(Rank.ShowTag.SHORT, false),
                Component.text(client.getName(), NamedTextColor.YELLOW)
        );
    }

    private void applyTeam(Scoreboard scoreboard, Player player, Rank rank) {
        removeManagedEntry(scoreboard, player.getName());

        Team team = scoreboard.getTeam(getTeamName(rank));
        if (team == null) {
            team = scoreboard.registerNewTeam(getTeamName(rank));
        }

        team.color(NamedTextColor.YELLOW);
        team.prefix(rank.getTag(Rank.ShowTag.SHORT, false));
        team.suffix(Component.empty());
        team.addEntry(player.getName());
    }

    private void removeManagedEntry(Scoreboard scoreboard, String playerName) {
        for (Rank rank : Rank.values()) {
            Team team = scoreboard.getTeam(getTeamName(rank));
            if (team != null && team.removeEntry(playerName)) {
                return;
            }
        }
    }

    private String getTeamName(Rank rank) {
        return TEAM_PREFIX + rank.getId();
    }
}
