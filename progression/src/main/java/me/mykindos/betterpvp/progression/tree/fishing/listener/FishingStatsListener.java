package me.mykindos.betterpvp.progression.tree.fishing.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.model.leaderboard.Leaderboard;
import me.mykindos.betterpvp.core.utilities.model.leaderboard.SortType;
import me.mykindos.betterpvp.core.utilities.model.leaderboard.TemporalSort;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStopFishingEvent;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.tree.fishing.repository.FishingRepository;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Comparator;
import java.util.Map;

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
            final Map<SortType, Integer> newPositionsWeight = fishing.getWeightLeaderboard().compute(event.getPlayer().getUniqueId(), data.getWeightCaught());
            final Map<SortType, Integer> newPositionsCount = fishing.getCountLeaderboard().compute(event.getPlayer().getUniqueId(), data.getFishCaught());
            announceIfPresent(event.getPlayer(), fishing.getWeightLeaderboard(), newPositionsWeight);
            announceIfPresent(event.getPlayer(), fishing.getCountLeaderboard(), newPositionsCount);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        }).thenRun(() -> repository.saveAsync(event.getPlayer()));
    }

    private void announceIfPresent(Player player, Leaderboard<?, ?> leaderboard, Map<SortType, Integer> newPositions) {
        if (newPositions.isEmpty()) {
            return;
        }
        final Map.Entry<TemporalSort, Integer> highestEntry = newPositions.entrySet().stream()
                .map(entry -> Map.entry((TemporalSort) entry.getKey(), entry.getValue()))
                .max(Map.Entry.comparingByKey(Comparator.comparing(TemporalSort::getDays)))
                .orElseThrow();

        final String playerName = player.getName();
        UtilMessage.simpleBroadcast("Fishing",
                "<dark_green>%s <green>has reached <dark_green>#%d</dark_green> on the %s %s leaderboard!",
                playerName,
                highestEntry.getValue(),
                highestEntry.getKey().getName(),
                leaderboard.getName());

        if (highestEntry.getKey() == TemporalSort.SEASONAL) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                UtilSound.playSound(onlinePlayer, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0F, 1.0F, true);
            }
        }
    }

}
