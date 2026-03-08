package me.mykindos.betterpvp.core.item.runeslot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.runeslot.loader.SupabaseRuneSlotDistributionLoader;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for rune slot distributions loaded from Supabase.
 * <p>
 * Caches distribution configurations in memory for performance. Each purity level has
 * associated weight maps that control how sockets and maxSockets are randomized
 * when items are attuned.
 * <p>
 * If Supabase is unavailable, falls back to hardcoded distributions that favor
 * higher socket counts for higher purities.
 */
@Singleton
@CustomLog
public class RuneSlotDistributionRegistry {

    private final Map<ItemPurity, RuneSlotDistribution> distributions = new ConcurrentHashMap<>();
    private final SupabaseRuneSlotDistributionLoader loader;

    @Inject
    public RuneSlotDistributionRegistry(SupabaseRuneSlotDistributionLoader loader) {
        this.loader = loader;
        reload();
    }

    /**
     * Reloads distribution configurations from Supabase.
     * Fills in any missing purities with fallback values.
     */
    public void reload() {
        log.info("Reloading rune slot distributions...").submit();

        // Reload credentials
        loader.reloadCredentials();

        // Clear existing distributions
        distributions.clear();

        // Load from Supabase
        Map<ItemPurity, RuneSlotDistribution> loaded = loader.loadDistributions();
        distributions.putAll(loaded);

        // Fill in any missing purities with fallback values
        for (ItemPurity purity : ItemPurity.values()) {
            if (!distributions.containsKey(purity)) {
                distributions.put(purity, createFallbackDistribution(purity));
            }
        }

        if (loaded.isEmpty()) {
            log.warn("No rune slot distributions loaded from Supabase, using fallback distributions").submit();
        } else {
            log.info("Loaded {} rune slot distributions", loaded.size()).submit();
        }
    }

    /**
     * Gets the distribution configuration for a specific purity level.
     * Always returns a valid distribution (uses fallback if not found).
     *
     * @param purity The purity level
     * @return The distribution configuration for this purity
     */
    @NotNull
    public RuneSlotDistribution getDistribution(@NotNull ItemPurity purity) {
        return distributions.getOrDefault(purity, createFallbackDistribution(purity));
    }

    /**
     * Gets all registered distribution configurations.
     *
     * @return Immutable map of all distributions
     */
    @NotNull
    public Map<ItemPurity, RuneSlotDistribution> getAllDistributions() {
        return Map.copyOf(distributions);
    }

    /**
     * Creates a hardcoded fallback distribution if Supabase is unavailable.
     * <p>
     * Distributions favor higher socket counts for higher purities:
     * <ul>
     *     <li>PITIFUL: Heavily favors 0-1 sockets</li>
     *     <li>FRAGILE: Favors 0-2 sockets</li>
     *     <li>MODERATE: Balanced distribution, slight favor toward 2</li>
     *     <li>POLISHED: Favors 2-3 sockets</li>
     *     <li>PRISTINE: Heavily favors 3-4 sockets</li>
     *     <li>PERFECT: Heavily favors 3-4 sockets and 4 maxSockets</li>
     * </ul>
     *
     * @param purity The purity level
     * @return Fallback distribution configuration
     */
    private RuneSlotDistribution createFallbackDistribution(ItemPurity purity) {
        return switch (purity) {
            case PITIFUL -> new RuneSlotDistribution(
                purity,
                Map.of(0, 70, 1, 20, 2, 8, 3, 2, 4, 0),
                Map.of(0, 60, 1, 25, 2, 12, 3, 3, 4, 0)
            );
            case FRAGILE -> new RuneSlotDistribution(
                purity,
                Map.of(0, 40, 1, 35, 2, 18, 3, 6, 4, 1),
                Map.of(0, 30, 1, 35, 2, 25, 3, 8, 4, 2)
            );
            case MODERATE -> new RuneSlotDistribution(
                purity,
                Map.of(0, 10, 1, 25, 2, 40, 3, 20, 4, 5),
                Map.of(0, 5, 1, 20, 2, 40, 3, 25, 4, 10)
            );
            case POLISHED -> new RuneSlotDistribution(
                purity,
                Map.of(0, 5, 1, 15, 2, 30, 3, 35, 4, 15),
                Map.of(0, 2, 1, 10, 2, 25, 3, 38, 4, 25)
            );
            case PRISTINE -> new RuneSlotDistribution(
                purity,
                Map.of(0, 2, 1, 8, 2, 20, 3, 40, 4, 30),
                Map.of(0, 1, 1, 4, 2, 15, 3, 35, 4, 45)
            );
            case PERFECT -> new RuneSlotDistribution(
                purity,
                Map.of(0, 1, 1, 2, 2, 10, 3, 35, 4, 52),
                Map.of(0, 1, 1, 2, 2, 7, 3, 25, 4, 65)
            );
        };
    }
}
