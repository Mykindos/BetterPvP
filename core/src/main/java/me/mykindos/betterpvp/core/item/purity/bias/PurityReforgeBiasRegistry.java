package me.mykindos.betterpvp.core.item.purity.bias;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.purity.loader.SupabasePurityReforgeBiasLoader;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for purity reforge bias configurations loaded from Supabase.
 * <p>
 * Caches bias configurations in memory for performance. Each purity level has
 * associated beta distribution parameters that control how stats are randomized
 * during reforging.
 * <p>
 * If Supabase is unavailable, falls back to hardcoded aggressive bias values:
 * - Lower purities (PITIFUL, FRAGILE) strongly favor minimum stats
 * - Higher purities (PRISTINE, PERFECT) strongly favor maximum stats
 * - MODERATE has a slight minimum bias
 */
@Singleton
@CustomLog
public class PurityReforgeBiasRegistry {

    private final Map<ItemPurity, PurityReforgeBias> biases = new ConcurrentHashMap<>();
    private final SupabasePurityReforgeBiasLoader loader;

    @Inject
    public PurityReforgeBiasRegistry(SupabasePurityReforgeBiasLoader loader) {
        this.loader = loader;
        reload();
    }

    /**
     * Reloads bias configurations from Supabase.
     * Fills in any missing purities with fallback values.
     */
    public void reload() {
        log.info("Reloading purity reforge biases...").submit();

        // Reload credentials
        loader.reloadCredentials();

        // Clear existing biases
        biases.clear();

        // Load from Supabase
        Map<ItemPurity, PurityReforgeBias> loaded = loader.loadBiases();
        biases.putAll(loaded);

        // Fill in any missing purities with fallback values
        for (ItemPurity purity : ItemPurity.values()) {
            if (!biases.containsKey(purity)) {
                biases.put(purity, createFallbackBias(purity));
            }
        }

        if (loaded.isEmpty()) {
            log.warn("No purity reforge biases loaded from Supabase, using fallback biases").submit();
        } else {
            log.info("Loaded {} purity reforge biases", loaded.size()).submit();
        }
    }

    /**
     * Gets the bias configuration for a specific purity level.
     * Always returns a valid bias (uses fallback if not found).
     *
     * @param purity The purity level
     * @return The bias configuration for this purity
     */
    @NotNull
    public PurityReforgeBias getBias(@NotNull ItemPurity purity) {
        return biases.getOrDefault(purity, createFallbackBias(purity));
    }

    /**
     * Gets all registered bias configurations.
     *
     * @return Immutable map of all biases
     */
    @NotNull
    public Map<ItemPurity, PurityReforgeBias> getAllBiases() {
        return Map.copyOf(biases);
    }

    /**
     * Creates a hardcoded fallback bias if Supabase is unavailable.
     * <p>
     * Aggressive curve shifted toward bottom:
     * <ul>
     *      <li>PITIFUL (α=0.3, β=3.0): Mean ≈ 0.09 → 91% toward min (very harsh)</li>
     *      <li>FRAGILE (α=0.5, β=2.0): Mean ≈ 0.20 → 80% toward min (harsh)</li>
     *      <li>MODERATE (α=1.0, β=1.5): Mean ≈ 0.40 → 60% toward min (slight penalty)</li>
     *      <li>POLISHED (α=1.5, β=1.0): Mean ≈ 0.60 → 60% toward max (slight bonus)</li>
     *      <li>PRISTINE (α=2.0, β=0.7): Mean ≈ 0.74 → 74% toward max (good bonus)</li>
     *      <li>PERFECT (α=2.5, β=0.5): Mean ≈ 0.83 → 83% toward max (excellent bonus)</li>
     * </ul>
     * @param purity The purity level
     * @return Fallback bias configuration
     */
    private PurityReforgeBias createFallbackBias(ItemPurity purity) {
        return switch (purity) {
            case PITIFUL -> new PurityReforgeBias(purity, 0.3, 3.0);   // Very harsh min bias
            case FRAGILE -> new PurityReforgeBias(purity, 0.5, 2.0);   // Strong min bias
            case MODERATE -> new PurityReforgeBias(purity, 1.0, 1.5);  // Slight min bias
            case POLISHED -> new PurityReforgeBias(purity, 1.5, 1.0);  // Slight max bias
            case PRISTINE -> new PurityReforgeBias(purity, 2.0, 0.7);  // Moderate max bias
            case PERFECT -> new PurityReforgeBias(purity, 2.5, 0.5);   // Strong max bias
        };
    }
}
