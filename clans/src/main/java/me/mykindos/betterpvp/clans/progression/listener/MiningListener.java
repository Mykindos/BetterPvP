package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.fields.event.FieldsInteractableUseEvent;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.clans.fields.model.FieldsOre;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Slf4j
public class MiningListener implements Listener {

    @Config(path = "progression-hook.mining.fieldsXpMultiplier", defaultValue = "5.0")
    private double xpMultiplier = 5;

    @Inject(optional = true)
    private Mining mining;

    @EventHandler
    public void onFieldsOreMine(FieldsInteractableUseEvent event) {
        final FieldsInteractable type = event.getType();
        if (!(type instanceof FieldsOre)) {
            return;
        }

        mining.getStatsRepository().getDataAsync(event.getPlayer()).whenComplete((miningData, throwable) -> {
            if (throwable != null) {
                log.error("Failed to update mining data for " + event.getPlayer().getName(), throwable);
                return;
            }

            final long defaultXp = mining.getMiningService().getExperience(event.getBlock().getBlock().getType());
            miningData.grantExperience((long) (defaultXp * xpMultiplier));
        }).exceptionally(throwable -> {
            log.error("Failed to update mining data for " + event.getPlayer().getName(), throwable);
            return null;
        });
    }

}
