package me.mykindos.betterpvp.progression.tree.fishing.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
@Singleton
public class FishingStatsListener implements Listener {

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;

    @Inject
    private GamerManager gamerManager;

    @Inject
    @Config(path = "fishing.xpPerPound", defaultValue = "0.10")
    private double xpPerPound;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCatch(PlayerStopFishingEvent event) {
        if (!event.getReason().equals(PlayerStopFishingEvent.FishingResult.CATCH)) {
            return;
        }

        final FishingLoot loot = event.getLoot();
        if (!(loot instanceof Fish fish)) {
            return;
        }

        final FishingRepository repository = fishing.getStatsRepository();
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

            // Leaderboard
            fishing.getWeightLeaderboard().add(event.getPlayer().getUniqueId(), (long) fish.getWeight()).whenComplete((result, throwable3) -> {
                if (throwable3 != null) {
                    log.error("Failed to add weight to leaderboard for player " + event.getPlayer().getName(), throwable3);
                    return;
                }

                fishing.getWeightLeaderboard().attemptAnnounce(event.getPlayer(),  result);
            });

            fishing.getCountLeaderboard().add(event.getPlayer().getUniqueId(), 1L).whenComplete((result, throwable2) -> {
                if (throwable2 != null) {
                    log.error("Failed to add count to leaderboard for player " + event.getPlayer().getName(), throwable2);
                    return;
                }

                fishing.getCountLeaderboard().attemptAnnounce(event.getPlayer(), result);
            });
        }).exceptionally(throwable -> {
            log.error("Failed to get progression data for player " + event.getPlayer().getName(), throwable);
            return null;
        }).thenRun(() -> repository.saveAsync(event.getPlayer()));
    }

}
