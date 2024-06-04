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
import me.mykindos.betterpvp.clans.clans.events.ClanDominanceChangeEvent;
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
import me.mykindos.betterpvp.core.utilities.UtilServer;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

@BPvPListener
@Singleton
public class PlayerName implements Listener {

    private final Clans clans;
    private final ClanManager clanManager;

    @Inject
    private PlayerName(Clans clans, ClanManager clanManager) {
        this.clans = clans;
        this.clanManager = clanManager;
    }

    public void broadcastChange(@Nullable Clan clan) {
        if (clan == null) return;
        clan.getMembersAsPlayers().forEach(this::broadcastChange);
    }

    public void broadcastChange(@NotNull Player player) {
        broadcastChange(player, Bukkit.getOnlinePlayers());
    }

    public void broadcastChange(@NotNull Player player, Collection<? extends Player> receivers) {
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(Clans.class), () -> {
            for (Player onlinePlayer : receivers) {
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
            if (active.removeEntry(player.getName())) {
                break;
            }
        }

        final String teamName = playerClan.map(Clan::getName).orElse("Wilderness");
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.addPlayer(player);

        team.color(relation.getPrimary());
        if (playerClan.isPresent()) {
            Clan clan = playerClan.get();
            team.prefix(Component.text(clan.getName(), relation.getSecondary()).appendSpace());
            team.suffix(Component.text(""));

            if (receiverClan.isPresent()) {
                if (relation == ClanRelation.ENEMY) {
                    team.suffix(clanManager.getSimpleDominanceString(clan, receiverClan.get()));
                }
            }

        } else {
            team.prefix(Component.empty().color(relation.getSecondary()));
            team.suffix(Component.empty());
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanLeave(final MemberLeaveClanEvent event) {
        UtilServer.runTaskLater(clans, () -> {
            this.broadcastChange(event.getPlayer());

            for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                this.sendChange(onlinePlayer, event.getPlayer());
            }
        }, 2L);

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanJoin(final MemberJoinClanEvent event) {
        UtilServer.runTaskLater(clans, () -> {
            this.broadcastChange(event.getPlayer());

            for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                this.sendChange(onlinePlayer, event.getPlayer());
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanDisband(final ClanDisbandEvent event) {
        UtilServer.runTaskLater(clans, () -> {
            this.broadcastChange(event.getPlayer());

            if(event.getPlayer() != null) {
                for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                    this.sendChange(onlinePlayer, event.getPlayer());
                }
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onClanCreate(final ClanCreateEvent event) {
        UtilServer.runTaskLater(clans, () -> {
            this.broadcastChange(event.getPlayer());

            for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                this.sendChange(onlinePlayer, event.getPlayer());
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onKick(final ClanKickMemberEvent event) {
        if (event.getTarget().getGamer().isOnline()) {
            Player player = event.getTarget().getGamer().getPlayer();
            if(player == null) return;
            UtilServer.runTaskLater(clans, () -> {
                this.broadcastChange(event.getPlayer());

                for (Player onlinePlayer : event.getPlayer().getServer().getOnlinePlayers()) {
                    this.sendChange(onlinePlayer, player);
                }
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTrust(final ClanTrustEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onNeutral(final ClanNeutralEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onUntrust(final ClanUntrustEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEnemy(final ClanEnemyEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onAlliance(final ClanAllianceEvent event) {
        this.broadcastChange(event.getClan());
        this.broadcastChange(event.getTargetClan());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPillageStart(final PillageStartEvent event) {
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillaged().getId()).orElse(null));
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillager().getId()).orElse(null));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPillageEnd(final PillageEndEvent event) {
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillaged().getId()).orElse(null));
        this.broadcastChange(this.clanManager.getClanById(event.getPillage().getPillager().getId()).orElse(null));
    }

    //@EventHandler
    //public void onDeath(CustomDeathEvent event) {
    //    if (event.getKiller() instanceof Player killer) {
    //        clanManager.getClanByPlayer(killer).ifPresent(this::broadcastChange);
    //    }
//
    //    if (event.getKilled() instanceof Player killed) {
    //        clanManager.getClanByPlayer(killed).ifPresent(this::broadcastChange);
    //    }
    //}

    @EventHandler
    public void onDominanceChange(ClanDominanceChangeEvent event) {
        if (event.getClan() instanceof Clan clan) {
            this.broadcastChange(clan);
        }
    }

}
