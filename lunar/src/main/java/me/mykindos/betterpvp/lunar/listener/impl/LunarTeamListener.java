package me.mykindos.betterpvp.lunar.listener.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.BukkitApollo;
import com.lunarclient.apollo.module.team.TeamMember;
import com.lunarclient.apollo.module.team.TeamModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.events.lunar.LunarClientEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.lunar.Lunar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Clans")
public class LunarTeamListener implements Listener {

    private final Lunar lunar;
    private final TeamModule teamModule;
    private final ClanManager clanManager;

    @Inject
    private LunarTeamListener(Lunar lunar) {
        this.lunar = lunar;
        this.clanManager = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClanManager.class);
        this.teamModule = Apollo.getModuleManager().getModule(TeamModule.class);
        this.teamModule.enable();
    }

    private void updateTeam(Player online) {
        if (online == null || !online.isOnline()) {
            return;
        }

        final Optional<ApolloPlayer> apolloOpt = Apollo.getPlayerManager().getPlayer(online.getUniqueId());
        if (apolloOpt.isEmpty()) {
            return; // Not a Lunar Client player
        }

        final ApolloPlayer apolloPlayer = apolloOpt.get();
        final Optional<Clan> clan = this.clanManager.getClanByPlayer(apolloPlayer.getUniqueId());
        // Empty clan means they are not in a clan, just remove them from the team
        if (clan.isEmpty()) {
            teamModule.resetTeamMembers(apolloPlayer);
            return;
        }

        final Clan c = clan.get();
        teamModule.updateTeamMembers(apolloPlayer, c.getMembersAsPlayers()
                .stream()
                .map(player -> TeamMember.builder()
                        .playerUuid(player.getUniqueId())
                        .displayName(player.displayName())
                        .markerColor(new Color(ClanRelation.SELF.getPrimary().value()))
                        .location(BukkitApollo.toApolloLocation(player.getLocation()))
                        .build())
                .toList());
    }

    @EventHandler
    public void onPlayerJoin(LunarClientEvent event) {
        if (event.isRegistered()) {
            this.updateTeam(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        this.updateTeam(event.getPlayer());

        for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
            this.updateTeam(onlinePlayer);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanLeave(final MemberLeaveClanEvent event) {
        UtilServer.runTaskLater(lunar, () -> {
            this.updateTeam(event.getPlayer());

            for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                this.updateTeam(onlinePlayer);
            }
        }, 2L);

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanJoin(final MemberJoinClanEvent event) {
        UtilServer.runTaskLater(lunar, () -> {
            this.updateTeam(event.getPlayer());

            for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                this.updateTeam(onlinePlayer);
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanDisband(final ClanDisbandEvent event) {
        UtilServer.runTaskLater(lunar, () -> {
            this.updateTeam(event.getPlayer());

            if (event.getPlayer() != null) {
                for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                    this.updateTeam(onlinePlayer);
                }
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanCreate(final ClanCreateEvent event) {
        UtilServer.runTaskLater(lunar, () -> {
            this.updateTeam(event.getPlayer());

            for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                this.updateTeam(onlinePlayer);
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onKick(final ClanKickMemberEvent event) {
        if (event.getTarget().getGamer().isOnline()) {
            Player player = event.getTarget().getGamer().getPlayer();
            if (player == null) return;
            UtilServer.runTaskLater(lunar, () -> {
                this.updateTeam(event.getPlayer());

                for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                    this.updateTeam(onlinePlayer);
                }
            }, 2L);
        }
    }

}
