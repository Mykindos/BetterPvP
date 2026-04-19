package me.mykindos.betterpvp.progression.profession.skill.woodcutting.forestflourisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

@BPvPListener
@Singleton
public class ForestFlourisherListener implements Listener {

    @Inject
    private ForestFlourisher skill;

    @EventHandler
    public void onPlayerPlantSapling(BlockPlaceEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onPlayerPlantSapling(event);
    }

    @UpdateEvent(delay = 60000L)
    public void increaseSaplingGrowthTime() {
        skill.increaseSaplingGrowthTime();
    }

    @UpdateEvent
    public void pollBlockToBoneMeal() {
        skill.pollBlockToBoneMeal();
    }
}
