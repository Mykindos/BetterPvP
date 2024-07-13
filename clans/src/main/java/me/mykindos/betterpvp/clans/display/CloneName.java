package me.mykindos.betterpvp.clans.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.UpdateCloneNameEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@BPvPListener
@Singleton
public class CloneName implements Listener {
    private final Clans clans;
    private final ClanManager clanManager;

    @Inject
    private CloneName(Clans clans, ClanManager clanManager) {
        this.clans = clans;
        this.clanManager = clanManager;
    }

    public void sendCloneChange(LivingEntity clone, Player player, @NotNull Player receiver) {
        if (player == null) return;

        final Scoreboard scoreboard = receiver.getScoreboard();

        final Optional<Clan> playerClan = this.clanManager.getClanByPlayer(player);
        final Optional<Clan> receiverClan = this.clanManager.getClanByPlayer(receiver);
        final ClanRelation relation = clanManager.getRelation(playerClan.orElse(null), receiverClan.orElse(null));

        // Determine team name based on player's clan or default to "Wilderness"
        final String teamName = playerClan.map(Clan::getName).orElse("Wilderness");
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }


        // Set team color and prefix/suffix based on relation and clans
        team.color(relation.getPrimary());
        if (playerClan.isPresent()) {
            Clan clan = playerClan.get();
            team.prefix(Component.text(clan.getName(), relation.getSecondary()).appendSpace());
            team.suffix(Component.text(""));

            if (receiverClan.isPresent() && relation == ClanRelation.ENEMY) {
                team.suffix(clanManager.getSimpleDominanceString(clan, receiverClan.get()));
            }
        } else {
            team.prefix(Component.empty().color(relation.getSecondary()));
            team.suffix(Component.empty());
        }


        clone.setCustomNameVisible(true); // Ensure custom name is visible
        clone.customName(Component.text(player.getName()));

        team.addEntry(clone.getUniqueId().toString());

        // Set the updated scoreboard for the receiver
        receiver.setScoreboard(scoreboard);
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onCloneUpdate(final UpdateCloneNameEvent event) {
        UtilServer.runTaskLater(clans, () -> {
            for (Player onlinePlayer : event.getSpawner().getServer().getOnlinePlayers()) {
                this.sendCloneChange(event.getClone(), event.getSpawner(), onlinePlayer);
            }
        }, 1L);
    }
}