package me.mykindos.betterpvp.hub.feature;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@BPvPListener
@Singleton
public class HubPlayerListListener implements Listener {

    private final Hub hub;
    private final ClientManager clientManager;

    @Inject
    public HubPlayerListListener(Hub hub, ClientManager clientManager) {
        this.hub = hub;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(ClientJoinEvent event) {
        UtilServer.runTaskLater(hub, () -> refreshJoin(event.getClient()), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(ClientQuitEvent event) {
        removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRankUpdate(ClientRankUpdateEvent event) {
        if (!event.getClient().isLoaded()) {
            return;
        }

        UtilServer.runTaskLater(hub, () -> refreshPlayer(event.getClient(), WrapperPlayServerTeams.TeamMode.UPDATE), 1L);
    }

    private void refreshJoin(Client joiningClient) {
        final Player joiningPlayer = joiningClient.getGamer().getPlayer();
        if (joiningPlayer == null) {
            return;
        }

        final List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> visibleEntries = new ArrayList<>();
        for (Client onlineClient : clientManager.getOnline()) {
            final Player onlinePlayer = onlineClient.getGamer().getPlayer();
            if (onlinePlayer == null) {
                continue;
            }

            if (onlinePlayer.equals(joiningPlayer)) {
                continue;
            }

            visibleEntries.add(createPlayerInfo(onlineClient));
        }

        sendDisplayNameUpdate(joiningPlayer, visibleEntries);
        for (Client onlineClient : clientManager.getOnline()) {
            final Player onlinePlayer = onlineClient.getGamer().getPlayer();
            if (onlinePlayer == null || onlinePlayer.equals(joiningPlayer)) {
                continue;
            }

            sendNameTagPacket(joiningPlayer, onlineClient, WrapperPlayServerTeams.TeamMode.CREATE);
        }

        refreshPlayer(joiningClient, WrapperPlayServerTeams.TeamMode.CREATE);
    }

    public void refreshPlayer(Client client) {
        refreshPlayer(client, WrapperPlayServerTeams.TeamMode.UPDATE);
    }

    private void refreshPlayer(Client client, WrapperPlayServerTeams.TeamMode teamMode) {
        final Player player = client.getGamer().getPlayer();
        if (player == null) {
            return;
        }

        final WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = createPlayerInfo(client);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendDisplayNameUpdate(viewer, List.of(playerInfo));
            sendNameTagPacket(viewer, client, teamMode);
        }
    }

    private Component buildPlayerListName(Client client) {
        return Component.join(
                JoinConfiguration.noSeparators(),
                client.getRank().getTag(Rank.ShowTag.SHORT, false),
                Component.text(client.getName(), NamedTextColor.YELLOW)
        );
    }

    private WrapperPlayServerPlayerInfoUpdate.PlayerInfo createPlayerInfo(Client client) {
        final Player player = client.getGamer().getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Cannot create player info for offline player");
        }

        final WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(player.getUniqueId());
        playerInfo.setDisplayName(buildPlayerListName(client));
        return playerInfo;
    }

    private void sendDisplayNameUpdate(Player viewer, Collection<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries) {
        if (entries.isEmpty()) {
            return;
        }

        final WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(Action.UPDATE_DISPLAY_NAME),
                List.copyOf(entries)
        );
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(packet);
    }

    private void sendNameTagPacket(Player viewer, Client client, WrapperPlayServerTeams.TeamMode teamMode) {
        final Player player = client.getGamer().getPlayer();
        if (player == null) {
            return;
        }

        final WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                getTeamName(player),
                teamMode,
                teamMode == WrapperPlayServerTeams.TeamMode.REMOVE
                        ? Optional.empty()
                        : Optional.of(buildTeamInfo(client)),
                teamMode == WrapperPlayServerTeams.TeamMode.UPDATE ? List.of() : List.of(player.getName())
        );
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(packet);
    }

    private void removePlayer(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            final WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                    getTeamName(player),
                    WrapperPlayServerTeams.TeamMode.REMOVE,
                    Optional.empty(),
                    List.of()
            );
            PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(packet);
        }
    }

    private WrapperPlayServerTeams.ScoreBoardTeamInfo buildTeamInfo(Client client) {
        return new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(),
                client.getRank().getTag(Rank.ShowTag.SHORT, false),
                Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                WrapperPlayServerTeams.CollisionRule.ALWAYS,
                NamedTextColor.YELLOW,
                WrapperPlayServerTeams.OptionData.NONE
        );
    }

    private String getTeamName(Player player) {
        return "hub" + player.getUniqueId().toString().replace("-", "").substring(0, 13);
    }
}
