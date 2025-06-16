package me.mykindos.betterpvp.core.client.stats.listeners;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.SkillStat;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillStatListeners implements Listener {
    private final ClientManager clientManager;

    public SkillStatListeners(ClientManager clientManager) {
        this.clientManager = clientManager;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillUse(PlayerUseSkillEvent event) {
        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        final StatContainer statContainer = client.getStatContainer();
        final SkillStat skillStat = SkillStat.builder()
                .action(SkillStat.Action.USE)
                .skill(event.getSkill())
                .level(event.getLevel())
                .build();
        statContainer.incrementStat(skillStat, 1);
    }
}
