package me.mykindos.betterpvp.core.loot;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import me.mykindos.betterpvp.core.loot.expression.ExpressionEngine;
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
     * The weighted loot entries.
     */
    @Builder.Default
    @NotNull List<@NotNull WeightedEntry> weightedEntries = new ArrayList<>();

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
     * Rolling happens in two phases. The <b>base phase</b> performs {@link #rollCountFunction} rolls using
     * this table's {@link #replacementStrategy}. The <b>bonus phase</b> then performs any rolls supplied
     * through the {@link ExpressionEngine#VAR_BONUS_ROLLS} context input, always drawing <i>with</i>
     * replacement from a fresh candidate pool so that a bonus roll is worth the same on a narrow table as
     * on a wide one. Entries that declare {@link ReplacementStrategy#WITHOUT_REPLACEMENT} on themselves
     * remain one-per-bundle across both phases.
     *
     * @param context The context in which the loot is being generated.
     * @return The generated loot bundle, not guaranteed to be populated with any entries.
     */
    public @NotNull LootBundle generateLoot(@NotNull LootContext context) {
        // 1. Guaranteed loot
        final List<Loot<?, ?>> loot = new ArrayList<>(guaranteedLoot);
        loot.removeIf(l -> !l.getCondition().test(context));

        // 2. Roll counts
        final int rolls = Math.max(0, rollCountFunction.apply(context));
        final int bonusRolls = resolveBonusRolls(context);
        if (weightedEntries.isEmpty() || (rolls <= 0 && bonusRolls <= 0)) {
            return finalizeBundle(context, loot);
        }

        // 3. Base phase, honouring this table's replacement strategy
        final int rolled = performRolls(context, loot, new ArrayList<>(weightedEntries), rolls, 0, true);

        // 4. Bonus phase, always with replacement against a fresh pool
        if (bonusRolls > 0) {
            performRolls(context, loot, bonusCandidates(loot), bonusRolls, rolled, false);
        }

        return finalizeBundle(context, loot);
    }

    /**
     * Performs {@code rolls} weighted draws against {@code candidates}, appending results to {@code loot}.
     *
     * @param startIndex          the roll index the first draw reports as {@link ExpressionEngine#VAR_ROLL_INDEX},
     *                            so the index stays monotonic across both phases.
     * @param applyTableStrategy  whether this table's {@link #replacementStrategy} may remove drawn candidates.
     *                            Per-entry overrides apply regardless.
     * @return the roll index the next phase should start from.
     */
    private int performRolls(@NotNull LootContext context, @NotNull List<Loot<?, ?>> loot,
                             @NotNull List<WeightedEntry> candidates, int rolls, int startIndex,
                             boolean applyTableStrategy) {
        if (candidates.isEmpty()) {
            return startIndex + rolls;
        }

        for (int i = 0; i < rolls; i++) {
            final LootContext rollContext = context.withInput(ExpressionEngine.VAR_ROLL_INDEX, startIndex + i)
                    .withInput(ExpressionEngine.VAR_BUNDLE_SIZE, loot.size());

            // base weights for this roll, then distribution adjustments
            final List<Integer> effectiveWeights = new ArrayList<>(candidates.size());
            for (WeightedEntry entry : candidates) {
                effectiveWeights.add(Math.max(0, entry.getWeight().applyAsInt(rollContext)));
            }
            applyWeightDistribution(candidates, effectiveWeights, rollContext.getSession().getProgress(), loot);

            int totalWeight = 0;
            for (int j = 0; j < candidates.size(); j++) {
                if (candidates.get(j).getLoot().getCondition().test(rollContext)) {
                    totalWeight += effectiveWeights.get(j);
                }
            }
            if (totalWeight <= 0) break;

            int r = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;
            for (int j = 0; j < candidates.size(); j++) {
                final WeightedEntry entry = candidates.get(j);
                final Loot<?, ?> candidate = entry.getLoot();

                if (!candidate.getCondition().test(rollContext)) continue;

                cumulative += effectiveWeights.get(j);
                if (r < cumulative) {
                    loot.add(candidate);

                    if (removesCandidate(candidate, applyTableStrategy)) {
                        candidates.remove(j);
                    }
                    break;
                }
            }
            if (candidates.isEmpty()) break;
        }

        return startIndex + rolls;
    }

    /**
     * Whether awarding {@code candidate} takes it out of the pool for the remainder of the phase. A loot
     * entry that declares its own strategy always wins; otherwise the table's strategy applies, and only
     * during the base phase.
     */
    private boolean removesCandidate(@NotNull Loot<?, ?> candidate, boolean applyTableStrategy) {
        final ReplacementStrategy own = candidate.getReplacementStrategy();
        if (own != null && own != ReplacementStrategy.UNSET) {
            return own == ReplacementStrategy.WITHOUT_REPLACEMENT;
        }
        return applyTableStrategy && this.replacementStrategy == ReplacementStrategy.WITHOUT_REPLACEMENT;
    }

    /**
     * Whether this entry declares itself one-per-bundle, as opposed to inheriting the table's strategy.
     */
    private boolean declaresOnePerBundle(@NotNull Loot<?, ?> candidate) {
        return candidate.getReplacementStrategy() == ReplacementStrategy.WITHOUT_REPLACEMENT;
    }

    /**
     * The candidate pool for the bonus phase: every weighted entry, minus those that opted into
     * {@link ReplacementStrategy#WITHOUT_REPLACEMENT} themselves and have already been awarded. The table's
     * own strategy is deliberately ignored here, which is what makes bonus rolls draw with replacement.
     */
    private @NotNull List<WeightedEntry> bonusCandidates(@NotNull List<Loot<?, ?>> awarded) {
        final List<WeightedEntry> candidates = new ArrayList<>(weightedEntries.size());
        for (WeightedEntry entry : weightedEntries) {
            final Loot<?, ?> candidate = entry.getLoot();
            if (declaresOnePerBundle(candidate) && awarded.contains(candidate)) continue;
            candidates.add(entry);
        }
        return candidates;
    }

    /**
     * Reads {@link ExpressionEngine#VAR_BONUS_ROLLS} from the context, coercing to a non-negative int.
     */
    private int resolveBonusRolls(@NotNull LootContext context) {
        final Object raw = context.getInput(ExpressionEngine.VAR_BONUS_ROLLS);
        if (raw instanceof Number number) {
            return Math.max(0, (int) Math.round(number.doubleValue()));
        }
        return 0;
    }

    private LootBundle finalizeBundle(LootContext context, List<Loot<?, ?>> loot) {
        final LootBundle bundle = new LootBundle(context, awardStrategy, loot);
        context.getSession().getProgress().history.add(bundle);
        return bundle;
    }

    private void applyWeightDistribution(List<WeightedEntry> candidates, List<Integer> weights,
                                         @Nullable LootProgress progress, List<Loot<?, ?>> awardedInThisBundle) {
        switch (weightDistributionStrategy) {
            case STATIC -> {
                // do nothing
            }
            case PITY -> {
                if (progress == null) return;
                for (int i = 0; i < candidates.size(); i++) {
                    Loot<?, ?> candidate = candidates.get(i).getLoot();

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

                    double delta = avg - w;
                    double shift = delta * factor;
                    if (scaleVar) {
                        double spread = Math.abs(delta);
                        double scale = (avg == 0.0) ? 1.0 : (spread / avg);
                        shift *= scale;
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

    /**
     * Returns a snapshot of the weighted entries keyed by {@link WeightedEntry#getDefaultWeight()}.
     * Intended for menu previews and persistence; runtime rolls use {@link #weightedEntries} directly.
     */
    public @NotNull Multimap<@NotNull Integer, @NotNull Loot<?, ?>> getWeightedLoot() {
        final Multimap<Integer, Loot<?, ?>> out = ArrayListMultimap.create();
        for (WeightedEntry entry : weightedEntries) {
            out.put(entry.getDefaultWeight(), entry.getLoot());
        }
        return out;
    }

    /**
     * Returns the static drop-chance preview based on {@link WeightedEntry#getDefaultWeight()}.
     * Expression-driven weights are represented by their default value here.
     */
    public Map<Loot<?, ?>, Float> getChances() {
        int sumWeights = 0;
        for (WeightedEntry entry : weightedEntries) sumWeights += entry.getDefaultWeight();
        final Map<Loot<?, ?>, Float> chances = new HashMap<>();
        for (Loot<?, ?> loot : this.guaranteedLoot) {
            chances.put(loot, 1.0f);
        }
        if (sumWeights <= 0) return chances;
        for (WeightedEntry entry : weightedEntries) {
            chances.put(entry.getLoot(), (float) entry.getDefaultWeight() / sumWeights);
        }
        return chances;
    }
}
