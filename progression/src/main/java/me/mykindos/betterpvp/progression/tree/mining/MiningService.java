package me.mykindos.betterpvp.progression.tree.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.tree.mining.data.MiningOresMinedLeaderboard;
import me.mykindos.betterpvp.progression.tree.mining.repository.MiningRepository;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.function.LongUnaryOperator;

@Slf4j
@Singleton
public class MiningService {

    @Setter
    @Getter
    private boolean enabled;

    @Inject
    private MiningRepository repository;

    @Inject
    private MiningOresMinedLeaderboard leaderboard;

    public long getExperience(Material material) {
        return repository.getExperienceFor(material);
    }

    public void attemptMineOre(Player player, Block block) {
        attemptMineOre(player, block, LongUnaryOperator.identity());
    }

    public void attemptMineOre(Player player, Block block, LongUnaryOperator experienceModifier) {
        if (!enabled) return;
        long experience = getExperience(block.getType());
        if (experience <= 0) {
            return; // Cancel if they don't get experience from this block
        }

        final long finalExperience = experienceModifier.applyAsLong(experience);
        repository.getDataAsync(player).whenComplete((miningData, throwable) -> {
            if (throwable != null) {
                log.error("Failed to update mining data for " + player.getName(), throwable);
                return;
            }

            // If it was player placed, grant 0 experience, so they get no XP and have a message sent anyway
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
            final boolean playerPlaced = pdc.has(CoreNamespaceKeys.PLAYER_PLACED_KEY);
            if (playerPlaced) {
                miningData.grantExperience(0, player);
                return;
            }

            // We give XP for any block that gives XP, even if it's not in the leaderboard
            miningData.grantExperience(finalExperience, player);
            miningData.saveOreMined(block); // Save this block to the database

            if (!repository.getLeaderboardBlocks().contains(block.getType())) {
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
