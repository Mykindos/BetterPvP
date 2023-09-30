package me.mykindos.betterpvp.progression.tree.fishing.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStopFishingEvent;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.tree.fishing.repository.FishingRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Slf4j
public class FishingStatsListener implements Listener {

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;

    @Inject
    private GamerManager gamerManager;

    @Inject
    @Config(path = "fishing.xpPerPound", defaultValue = "0.05")
    private double xpPerPound = 0.05;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCatch(PlayerStopFishingEvent event) {
        if (!event.getReason().equals(PlayerStopFishingEvent.FishingResult.CATCH)) {
            return;
        }

        final FishingLoot loot = event.getLoot();
        if (!(loot instanceof Fish fish)) {
            return;
        }

        final FishingRepository repository = fishing.getRepository();
        repository.getDataAsync(event.getPlayer()).whenComplete((data, throwable) -> {
            if (throwable != null) {
                log.error("Failed to get progression data for player " + event.getPlayer().getName(), throwable);
                return;
            }

            // Experience
            int xp = (int) (fish.getWeight() * xpPerPound);
            if (xp > 0) {
                data.grantExperience(xp, event.getPlayer());
            }

            // Stats
            data.addFish(fish);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        }).thenRun(() -> repository.save(event.getPlayer()));
    }

}
