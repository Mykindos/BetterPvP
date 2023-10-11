package me.mykindos.betterpvp.progression.tree.mining.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.tree.mining.MiningService;
import me.mykindos.betterpvp.progression.tree.mining.data.MiningOresMinedLeaderboard;
import me.mykindos.betterpvp.progression.tree.mining.repository.MiningRepository;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@BPvPListener
@Slf4j
@Singleton
public class MiningStatsListener implements Listener {

    @Inject
    private MiningService service;

    @Inject
    private MiningRepository repository;

    @Inject
    private MiningOresMinedLeaderboard leaderboard;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        final long experience = service.getExperience(block.getType());
        if (experience <= 0) {
            return; // Cancel if they don't get experience from this block
        }

        final Player player = event.getPlayer();
        repository.getDataAsync(player).whenComplete((miningData, throwable) -> {
            if (throwable != null) {
                log.error("Failed to update mining data for " + player.getName(), throwable);
                return;
            }

            // We give XP for any block that gives XP, even if it's not in the leaderboard
            miningData.grantExperience(experience, player);
            miningData.saveOreMined(block); // Save this block to the database

            if (!service.getLeaderboardBlocks().contains(block.getType())) {
                return;
            }

            // But only add the stat if it's in the leaderboard
            miningData.increaseMinedStat(block); // Only give the stat if it's in the threshold
            leaderboard.add(player.getUniqueId(), 1L).whenComplete((result, throwable2) -> {
                if (throwable2 != null) {
                    log.error("Failed to add ore count to leaderboard for player " + player.getName(), throwable2);
                    return;
                }

                leaderboard.attemptAnnounce(player, result);
            });

        }).exceptionally(throwable -> {
            log.error("Failed to update mining data for " + player.getName(), throwable);
            return null;
        }).thenRun(() -> repository.saveAsync(player));
    }

}
