package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ChampionsSkillStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class SkillStatListener implements Listener {
    private final ClientManager clientManager;

    @Inject
    public SkillStatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillEquip(SkillEquipEvent event) {
        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        final StatContainer statContainer = client.getStatContainer();
        final ChampionsSkillStat skillStat = ChampionsSkillStat.builder()
                .action(ChampionsSkillStat.Action.USE)
                .skill(event.getSkill())
                .build();
        statContainer.incrementStat(skillStat, 1);
    }
}
