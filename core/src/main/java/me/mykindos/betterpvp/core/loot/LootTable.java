package me.mykindos.betterpvp.core.loot;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
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
     * The strategy for awarding loot. See {@link AwardStrategy}.
     */
    @Builder.Default
    @NotNull AwardStrategy awardStrategy = AwardStrategy.DEFAULT;

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
            final LootBundle bundle = new LootBundle(context, awardStrategy, loot);
            context.getSession().getProgress().history.add(bundle);
            return bundle;
        }

        // 3. Flatten weighted loot into entries with base weight
        List<Loot<? ,?>> candidates = new ArrayList<>();
        List<Integer> baseWeights = new ArrayList<>();
        for (var entry : weightedLoot.entries()) {
            int weight = entry.getKey();
            Loot<? ,?> l = entry.getValue();
            if (weight <= 0) continue;
            candidates.add(l);
            baseWeights.add(weight);
        }

        if (candidates.isEmpty()) {
            final LootBundle bundle = new LootBundle(context,awardStrategy, loot);
            context.getSession().getProgress().history.add(bundle);
            return bundle;
        }

        // 4. Perform rolls
        for (int i = 0; i < rolls; i++) {
            // copy base -> effective, then apply distribution for this moment-in-bundle
            final List<Integer> effectiveWeights = new ArrayList<>(baseWeights);
            applyWeightDistribution(candidates, effectiveWeights, context.getSession().getProgress(), loot);

            // total must be from effective weights
            int totalWeight = 0;
            for (int j = 0; j < candidates.size(); j++) {
                if (candidates.get(j).getCondition().test(context)) {
                    totalWeight += effectiveWeights.get(j);
                }
            }
            if (totalWeight <= 0) break;

            int r = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;
            for (int j = 0; j < candidates.size(); j++) {
                final Loot<?, ?> candidate = candidates.get(j);

                // do not count weight for candidates that fail the condition
                if (!candidate.getCondition().test(context)) continue;

                cumulative += effectiveWeights.get(j);
                if (r < cumulative) {
                    loot.add(candidate);

                    final ReplacementStrategy strategy = candidate.getReplacementStrategy()
                            .orElse(this.replacementStrategy);
                    if (strategy == ReplacementStrategy.WITHOUT_REPLACEMENT) {
                        candidates.remove(j);
                        baseWeights.remove(j); // <-- remove from base, not effective
                    }
                    break;
                }
            }
            if (candidates.isEmpty()) break;
        }

        final LootBundle bundle = new LootBundle(context,awardStrategy, loot);
        context.getSession().getProgress().history.add(bundle);
        return bundle;
    }

    private void applyWeightDistribution(List<Loot<? ,?>> candidates, List<Integer> weights,
                                         @Nullable LootProgress progress, List<Loot<?, ?>> awardedInThisBundle) {
        switch (weightDistributionStrategy) {
            case STATIC -> {
                // do nothing
            }
            case PITY -> {
                if (progress == null) return;
                for (int i = 0; i < candidates.size(); i++) {
                    Loot<? ,?> candidate = candidates.get(i);

                    // suppress pity if this bundle already contains this loot
                    if (awardedInThisBundle.contains(candidate)) continue;

                    for (PityRule rule : pityRules) {
                        if (rule.getLoot().equals(candidate)) {
                            int failedRolls = progress.getFailedRolls(candidate);
                            if (failedRolls > 0) {
                                int increments = failedRolls / rule.getMaxAttempts();
                                int extra = increments * rule.getWeightIncrement();
                                if (extra > 0) {
                                    weights.set(i, weights.get(i) + extra);
                                }
                            }
                        }
                    }
                }
            }
            case PROGRESSIVE -> {
                if (progress == null || weights.isEmpty()) return;

                // mean of active weights (>=0)
                long sum = 0;
                int n = 0;
                for (int w : weights) { if (w > 0) { sum += w; n++; } }
                if (n == 0) return;
                double avg = (double) sum / n;

                final int maxShift = progressiveWeightConfig.getMaxShift();
                final double factor = progressiveWeightConfig.getShiftFactor();
                final boolean scaleVar = progressiveWeightConfig.isEnableVarianceScaling();

                for (int j = 0; j < weights.size(); j++) {
                    int w = weights.get(j);
                    if (w <= 0) continue;

                    double delta = avg - w;                  // move toward mean
                    double shift = delta * factor;           // base shift
                    if (scaleVar) {
                        double spread = Math.abs(delta);
                        double scale = (avg == 0.0) ? 1.0 : (spread / avg);
                        shift *= scale;                      // variance scaling
                    }
                    if (maxShift > 0) {
                        if (shift >  maxShift) shift =  maxShift;
                        if (shift < -maxShift) shift = -maxShift;
                    }

                    int newW = (int) Math.max(0, Math.round(w + shift));
                    weights.set(j, newW);
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
