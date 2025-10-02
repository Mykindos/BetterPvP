package me.mykindos.betterpvp.core.loot;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a loot table. A loot table is a collection of {@link Loot} instances that can be generated
 * upon a {@link LootContext} request.
 */
@Value()
@Builder(access = AccessLevel.PUBLIC)
public class LootTable {

    /**
     * The unique identifier or name for the loot table.
     */
    @NotNull String id;

    /**
     * The strategy for replacing existing loot. See {@link ReplacementStrategy}.
     */
    @Builder.Default
    @NotNull ReplacementStrategy replacementStrategy = ReplacementStrategy.WITH_REPLACEMENT;

    /**
     * The function to use for rolling the number of loot entries to generate. See {@link RollCountFunction}.
     */
    @Builder.Default
    @NotNull RollCountFunction rollCountFunction = RollCountFunction.constant(1);

    /**
     * The guaranteed loot entries. These entries will always be generated regardless of the roll count.
     */
    @Builder.Default
    @NotNull List<@NotNull Loot<?, ?>> guaranteedLoot = new ArrayList<>();

    /**
     * The weighted loot entries. These entries will be generated based on their weight.
     * <p>
     * Higher weights will result in a higher chance of being generated.
     */
    @Builder.Default
    @NotNull Multimap<@NotNull Integer, @NotNull Loot<?, ?>> weightedLoot = ArrayListMultimap.create();

    /**
     * The pity rules for this loot table. See {@link PityRule}.
     * This only applies when {@link #weightDistributionStrategy} is {@link WeightDistributionStrategy#PITY}.
     */
    @Builder.Default
    @NotNull List<PityRule> pityRules = new ArrayList<>();

    /**
     * The strategy to distribute weights across multiple entries through a {@link LootProgress}.
     */
    @Builder.Default
    @NotNull WeightDistributionStrategy weightDistributionStrategy = WeightDistributionStrategy.STATIC;

    /**
     * Progressive weight distribution configuration.
     * Allows fine-tuning of how weights are adjusted towards the center.
     * This only applies when {@link #weightDistributionStrategy} is {@link WeightDistributionStrategy#PROGRESSIVE}.
     * See {@link ProgressiveWeightConfig}.
     */
    @Builder.Default
    @NotNull ProgressiveWeightConfig progressiveWeightConfig = ProgressiveWeightConfig.builder().build();

    /**
     * Generates a loot bundle based on this loot table.
     * <p>
     *     Two types of loot are generated:
     *     <ul>
     *         <li>Guaranteed loot: that is always generated regardless of the roll count.</li>
     *         <li>Weighted loot: that is generated based on their weight.</li>
     *     </ul>
     * </p>
     * <br>
     * <p>
     *     The steps to generate loot are the following:
     *     <ol>
     *         <li>Generate guaranteed loot.</li>
     *         <li>Call the {@link #rollCountFunction} to get the number of loot entries to generate.</li>
     *         <li>Make an aggregate list of weighted loot entries based on their weight.</li>
     *         <li>Sum the weights of all weighted loot entries.</li>
     *         <li>Generate a random number between 0 and the sum of all weighted loot entry weights.</li>
     *         <li>Iterate over the aggregate list and pick the first entry whose weight is greater than the random number.</li>
     *         <li>Add the picked entry to the generated loot bundle.</li>
     *         <li>Repeat steps 5-7 until the roll count has been reached.</li>
     *     </ol>
     * </p>
     *
     * @param context The context in which the loot is being generated.
     *
     * @return The generated loot bundle, not guaranteed to be populated with any entries.
     */
    public @NotNull LootBundle generateLoot(@NotNull LootContext context) {
        // 1. Guaranteed loot
        final List<Loot<? ,?>> loot = new ArrayList<>(guaranteedLoot);
        loot.removeIf(l -> !l.getCondition().test(context));

        // 2. Roll count
        int rolls = rollCountFunction.apply(context);
        if (rolls <= 0) {
            final LootBundle bundle = new LootBundle(context, loot);
            context.getSession().getProgress().history.add(bundle);
            return bundle;
        }

        // 3. Flatten weighted loot into entries with base weight
        List<Loot<? ,?>> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (var entry : weightedLoot.entries()) {
            int weight = entry.getKey();
            Loot<? ,?> l = entry.getValue();
            if (weight <= 0) continue;
            candidates.add(l);
            weights.add(weight);
        }

        if (candidates.isEmpty()) {
            final LootBundle bundle = new LootBundle(context, loot);
            context.getSession().getProgress().history.add(bundle);
            return bundle;
        }

        // 4. Adjust weights based on strategy
        applyWeightDistribution(candidates, weights, context.getSession().getProgress());

        // Compute initial total
        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            final LootBundle bundle = new LootBundle(context, loot);
            context.getSession().getProgress().history.add(bundle);
            return bundle;
        }

        // 5. Perform rolls
        for (int i = 0; i < rolls; i++) {
            int r = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;
            for (int j = 0; j < candidates.size(); j++) {
                cumulative += weights.get(j);
                final Loot<?, ?> candidate = candidates.get(j);
                if (!candidate.getCondition().test(context)) {
                    continue;
                }

                if (r < cumulative) {
                    loot.add(candidate);

                    // Replacement handling
                    final ReplacementStrategy strategy = candidate.getReplacementStrategy()
                            .orElse(this.replacementStrategy);
                    if (strategy == ReplacementStrategy.WITHOUT_REPLACEMENT) {
                        totalWeight -= weights.get(j);
                        candidates.remove(j);
                        weights.remove(j);
                    }
                    break;
                }
            }
            if (candidates.isEmpty()) break;
        }

        final LootBundle bundle = new LootBundle(context, loot);
        context.getSession().getProgress().history.add(bundle);
        return bundle;
    }

    private void applyWeightDistribution(List<Loot<? ,?>> candidates, List<Integer> weights, @Nullable LootProgress progress) {
        switch (weightDistributionStrategy) {
            case STATIC -> {
                // do nothing
            }
            case PITY -> {
                if (progress == null) return;
                for (int i = 0; i < candidates.size(); i++) {
                    Loot<? ,?> candidate = candidates.get(i);
                    for (PityRule rule : pityRules) {
                        if (rule.getLoot().equals(candidate)) {
                            int failedRolls = progress.getFailedRolls(candidate);
                            if (failedRolls > 0) {
                                int increments = failedRolls / rule.getMaxAttempts();
                                int extra = increments * rule.getWeightIncrement();
                                weights.set(i, weights.get(i) + extra);
                            }
                        }
                    }
                }
            }
            case PROGRESSIVE -> {
                if (progress == null) return;

                // Calculate center and total variance
                int center = weights.stream().mapToInt(Integer::intValue).sum() / weights.size();

                for (int i = 0; i < progress.getHistory().size(); i++) {
                    int totalVariance = weights.stream()
                            .mapToInt(w -> Math.abs(w - center))
                            .sum();

                    // Apply progressive weight adjustment
                    for (int j = 0; j < weights.size(); j++) {
                        int currentWeight = weights.get(j);
                        int variance = Math.abs(currentWeight - center);

                        // Calculate shift based on configuration
                        double normalizedVariance = (double) variance / totalVariance;
                        int shift = (int) (progressiveWeightConfig.getMaxShift() * normalizedVariance);

                        // Adjust weight towards center
                        if (currentWeight < center) {
                            currentWeight = Math.min(center, currentWeight + shift);
                        } else if (currentWeight > center) {
                            currentWeight = Math.max(center, currentWeight - shift);
                        }

                        weights.set(j, currentWeight);
                    }
                }
            }
        }
    }

    public @NotNull Multimap<@NotNull Integer, @NotNull Loot<?, ?>> getWeightedLoot() {
        return ArrayListMultimap.create(weightedLoot);
    }

    public Map<Loot<?, ?>, Float> getChances() {
        int sumWeights = this.weightedLoot.keys().stream().mapToInt(Integer::intValue).sum();
        final Map<Loot<?, ?>, Float> chances = new HashMap<>();
        for (Loot<?, ?> loot : this.guaranteedLoot) {
            chances.put(loot, 1.0f);
        }

        for (Map.Entry<@NotNull Integer, @NotNull Loot<?, ?>> entry : this.weightedLoot.entries()) {
            float chance = (float) entry.getKey() / sumWeights;
            chances.put(entry.getValue(), chance);
        }
        return chances;
    }
}
