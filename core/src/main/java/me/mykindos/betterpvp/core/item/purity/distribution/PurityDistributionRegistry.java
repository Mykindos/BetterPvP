package me.mykindos.betterpvp.core.item.purity.distribution;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.purity.loader.SupabasePurityDistributionLoader;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for purity distributions loaded from Supabase.
 * Caches distributions in memory for performance.
 */
@Singleton
@CustomLog
public class PurityDistributionRegistry {

    private final Map<String, PurityDistribution> distributions = new ConcurrentHashMap<>();
    private PurityDistribution defaultDistribution;
    private final SupabasePurityDistributionLoader loader;

    @Inject
    public PurityDistributionRegistry(SupabasePurityDistributionLoader loader) {
        this.loader = loader;
        reload();
    }

    /**
     * Reloads distributions from Supabase.
     */
    public void reload() {
        log.info("Reloading purity distributions...").submit();

        // Reload credentials
        loader.reloadCredentials();

        // Clear existing distributions
        distributions.clear();

        // Load from Supabase
        Map<String, PurityDistribution> loaded = loader.loadDistributions();
        distributions.putAll(loaded);

        // Set default distribution
        defaultDistribution = distributions.getOrDefault("default", createFallbackDistribution());

        if (distributions.isEmpty()) {
            log.warn("No purity distributions loaded from Supabase, using fallback distribution").submit();
        } else {
            log.info("Loaded {} purity distributions", distributions.size()).submit();
        }
    }

    /**
     * Gets a distribution by name.
     *
     * @param name The distribution name
     * @return The distribution, or the default distribution if not found
     */
    @NotNull
    public PurityDistribution getDistribution(@NotNull String name) {
        return distributions.getOrDefault(name, defaultDistribution);
    }

    /**
     * Gets the default purity distribution.
     *
     * @return The default distribution
     */
    @NotNull
    public PurityDistribution getDefaultDistribution() {
        return defaultDistribution;
    }

    /**
     * Gets all registered distributions.
     *
     * @return Immutable map of all distributions
     */
    @NotNull
    public Map<String, PurityDistribution> getAllDistributions() {
        return Map.copyOf(distributions);
    }

    /**
     * Creates a hardcoded fallback distribution if Supabase is unavailable.
     * Bell curve distribution: prioritizes Fragile/Moderate, Perfect extremely rare.
     *
     * @return Fallback PurityDistribution
     */
    private PurityDistribution createFallbackDistribution() {
        Map<ItemPurity, Integer> weights = new HashMap<>();
        weights.put(ItemPurity.PITIFUL, 5);
        weights.put(ItemPurity.FRAGILE, 25);
        weights.put(ItemPurity.MODERATE, 40);
        weights.put(ItemPurity.POLISHED, 20);
        weights.put(ItemPurity.PRISTINE, 8);
        weights.put(ItemPurity.PERFECT, 2);
        return new PurityDistribution("fallback", weights);
    }
}
