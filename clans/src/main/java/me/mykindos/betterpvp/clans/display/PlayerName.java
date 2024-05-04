package me.mykindos.betterpvp.clans.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.events.ClanAllianceEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanEnemyEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanNeutralEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanTrustEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanUntrustEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageEndEvent;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
public class PlayerName implements Listener {

    private final ClanManager clanManager;

    @Inject
    private PlayerName(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public void broadcastChange(@NotNull Clan clan) {
        clan.getMembersAsPlayers().forEach(this::broadcastChange);
    }

    public void broadcastChange(@NotNull Player player) {
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(Clans.class), () -> {
            for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                this.sendChange(player, onlinePlayer);
            }
        }, 2L);
    }

    public void sendChange(@NotNull Player player, @NotNull Player receiver) {
        final Scoreboard scoreboard = receiver.getScoreboard();

        final Optional<Clan> playerClan = this.clanManager.getClanByPlayer(player);
        final Optional<Clan> receiverClan = this.clanManager.getClanByPlayer(receiver);
        final ClanRelation relation = clanManager.getRelation(playerClan.orElse(null), receiverClan.orElse(null));

        for (Team active : scoreboard.getTeams()) {
            if (active.hasPlayer(player)) {
                active.removePlayer(player);
            }
        }

        final String teamName = playerClan.map(Clan::getName).orElse("Wilderness");
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        if (!team.hasPlayer(player)) {
            team.addPlayer(player);
        }

        team.color(relation.getPrimary());
        if (playerClan.isPresent()) {
            team.prefix(Component.text(playerClan.get().getName(), relation.getSecondary()).appendSpace());
        } else {
            team.prefix(Component.empty().color(relation.getSecondary()));
        }

        receiver.setScoreboard(scoreboard);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        this.broadcastChange(event.getPlayer());

        for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
            this.sendChange(onlinePlayer, event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onClanLeave(final MemberLeaveClanEvent event) {
        this.broadcastChange(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onClanJoin(final MemberJoinClanEvent event) {
        this.broadcastChange(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    private void onClanDisband(final ClanDisbandEvent event) {
        this.broadcastChange(event.getClan());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onClanCreate(final ClanCreateEvent event) {
        this.broadcastChange(event.getClan());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onKick(final ClanKickMemberEvent event) {
        if (event.getTarget().getGamer().isOnline()) {
            this.broadcastChange(Objects.requireNonNull(event.getTarget().getGamer().getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onTrust(final ClanTrustEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onNeutral(final ClanNeutralEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onUntrust(final ClanUntrustEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onEnemy(final ClanEnemyEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onAlliance(final ClanAllianceEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPillageStart(final PillageStartEvent event) {
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillaged().getId()).orElseThrow());
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillager().getId()).orElseThrow());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPillageEnd(final PillageEndEvent event) {
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillaged().getId()).orElseThrow());
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillager().getId()).orElseThrow());
    }

}
