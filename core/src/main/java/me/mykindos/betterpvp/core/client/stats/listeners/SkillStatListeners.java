package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
@CustomLog
public class SkillStatListeners implements Listener {
    private final ClientManager clientManager;

    @Inject
    public SkillStatListeners(ClientManager clientManager) {
        this.clientManager = clientManager;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillUse(PlayerUseSkillEvent event) {
        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        final StatContainer statContainer = client.getStatContainer();
        final ChampionsSkillStat skillStat = ChampionsSkillStat.builder()
                .action(ChampionsSkillStat.Action.USE)
                .skill(event.getSkill())
                .level(event.getLevel())
                .build();
        statContainer.incrementStat(skillStat, 1);

    }
}
