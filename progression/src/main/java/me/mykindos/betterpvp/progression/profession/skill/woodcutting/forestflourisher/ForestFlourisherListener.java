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
    private ForestFlourisher attribute;

    @EventHandler
    public void onPlayerPlantSapling(BlockPlaceEvent event) {
        if (!attribute.doesPlayerHaveAttribute(event.getPlayer())) return;
        attribute.onPlayerPlantSapling(event);
    }

    @UpdateEvent(delay = 60000L)
    public void increaseSaplingGrowthTime() {
        attribute.increaseSaplingGrowthTime();
    }

    @UpdateEvent
    public void pollBlockToBoneMeal() {
        attribute.pollBlockToBoneMeal();
    }
}
