package me.mykindos.betterpvp.progression.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.model.stats.ProgressionStatsRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a progression tree, with skills, buffs, and ways of gaining experience.
 */
public abstract class ProgressionTree implements ConfigAccessor {

    private final Set<ProgressionPerk> perks = new HashSet<>();

    /**
     * Get the name of the progression tree
     *
     * @return The name of the progression tree
     */
    public abstract @NotNull String getName();

    public abstract @NotNull ProgressionStatsRepository<? extends ProgressionTree, ? extends ProgressionData<?>> getStatsRepository();

    public final void addPerk(ProgressionPerk perk) {
        perks.add(perk);
    }

    public final Set<ProgressionPerk> getPerks() {
        return perks;
    }

    public final CompletableFuture<Set<ProgressionPerk>> getPerks(Player player) {
        final Set<ProgressionPerk> result = new HashSet<>();
        return getStatsRepository().getDataAsync(player).whenComplete((data, throwable) -> {
            for (ProgressionPerk perk : getPerks()) {
                if (perk.canUse(player, data)) {
                    result.add(perk);
                }
            }
        }).thenApply(data -> result);
    }

    public final CompletableFuture<Integer> getLevel(Player player) {
        return getStatsRepository().getDataAsync(player).thenApply(ProgressionData::getLevel);
    }

    public final CompletableFuture<Boolean> hasPerk(Player player, Class<?> perk) {
        return getPerks(player).thenApply(owned -> owned.stream().anyMatch(perk::isInstance));
    }
}