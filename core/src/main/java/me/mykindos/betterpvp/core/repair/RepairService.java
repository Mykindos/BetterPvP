package me.mykindos.betterpvp.core.repair;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.ReinforcementComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.RepairableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves whether the items currently on an anvil constitute a valid repair, and
 * computes the resulting {@link RepairPlan}.
 * <br>
 * A repair requires: exactly one damaged item carrying both a {@link DurabilityComponent}
 * and a {@link RepairableComponent}, plus a sufficient number of matching-tier
 * {@link ReinforcementComponent} items. The required count scales with the target's
 * {@link ItemPurity}: {@code reinforcementsPerPurityOrder * (purity.level + 1)} —
 * defaults to {@code 2 * (level + 1)}, so Pitiful=2, Fragile=4, Moderate=6, Polished=8,
 * Pristine=10, Perfect=12. Purity only influences cost when the item is <i>attuned</i>;
 * an unattuned item — or one lacking a {@link PurityComponent} entirely — is billed as
 * {@link ItemPurity#FRAGILE} so the cost cannot leak a hidden purity roll.
 * <br>
 * Any other items on the anvil are returned to the player untouched — they are neither
 * required nor consumed. Roles are derived purely from item identity/type; anvil slots
 * and placement order are never consulted.
 */
@Singleton
public class RepairService {

    @Inject
    @Config(path = "repair.requiredSwings", defaultValue = "3")
    private int requiredSwings;

    @Inject
    @Config(path = "repair.lifetimeMultiplier", defaultValue = "3")
    private int lifetimeMultiplier;

    /** Durability restored per repair execution, as a fraction of the item's max durability. */
    @Inject
    @Config(path = "repair.baseFraction", defaultValue = "0.10")
    private double baseFraction;

    /** Reinforcements consumed per repair execution per purity order (level + 1). */
    @Inject
    @Config(path = "repair.reinforcementsPerPurityOrder", defaultValue = "3")
    private int reinforcementsPerPurityOrder;

    /**
     * Attempts to resolve a repair from the given anvil contents.
     *
     * @param items the items on the anvil (keys/slots are ignored — only values matter)
     * @return a plan describing the repair, or empty if these items are not a repair.
     * A plan with {@link RepairPlan#isCanRepair()} == false is still returned for an
     * exhausted item so the anvil can show the "Not Repairable" indicator.
     */
    public @NotNull Optional<RepairPlan> resolve(@NotNull Map<Integer, ItemInstance> items) {
        final List<ItemInstance> contents = new ArrayList<>();
        for (ItemInstance instance : items.values()) {
            if (instance != null) {
                contents.add(instance);
            }
        }

        // Find the single repairable target (carries both durability and repairable).
        ItemInstance target = null;
        for (ItemInstance instance : contents) {
            final boolean repairable = instance.getComponent(DurabilityComponent.class).isPresent()
                    && instance.getComponent(RepairableComponent.class).isPresent();
            if (!repairable) {
                continue;
            }
            if (target != null) {
                return Optional.empty(); // Ambiguous — more than one repairable item.
            }
            target = instance;
        }

        if (target == null) {
            return Optional.empty();
        }

        final DurabilityComponent durability = target.getComponent(DurabilityComponent.class).orElseThrow();
        final RepairableComponent repairable = target.getComponent(RepairableComponent.class).orElseThrow();
        final int maxDamage = durability.getMaxDamage();
        final ItemRarity rarity = target.getRarity();

        // Nothing to repair — let the anvil fall back to showing no operation.
        if (durability.getDamage() <= 0) {
            return Optional.empty();
        }

        final int requiredReinforcements = requiredReinforcements(target);

        // Exhausted lifetime budget takes priority: the item can never be repaired again,
        // regardless of how much catalyst is present. Surface that state first so the
        // hologram never misleads the player into stockpiling reinforcements.
        if (!repairable.isRepairable(maxDamage, lifetimeMultiplier)) {
            return Optional.of(new RepairPlan(target, durability, repairable, rarity, 0,
                    false, RepairPlan.Status.EXHAUSTED, requiredSwings, requiredReinforcements,
                    Collections.emptyList()));
        }

        // Collect every matching-tier reinforcement on the anvil. Cost scales with purity:
        // higher-purity items demand more of the same tier reinforcement per execution.
        final List<ItemInstance> reinforcements = new ArrayList<>();
        int reinforcementUnits = 0;
        for (ItemInstance instance : contents) {
            if (instance == target) {
                continue;
            }
            final boolean isMatching = instance.getComponent(ReinforcementComponent.class)
                    .map(reinforcement -> reinforcement.getTier() == rarity)
                    .orElse(false);
            if (!isMatching) {
                continue; // Non-reinforcement items are ignored and returned untouched.
            }
            reinforcements.add(instance);
            reinforcementUnits += Math.max(1, instance.createItemStack().getAmount());
        }

        // Not enough matching catalyst — still surface a plan so the anvil can show the
        // required cost. The plan does not mutate items; complete() is a no-op for this
        // status. AnvilOperationResolver checks recipes before us, so a real recipe will
        // never be shadowed by this informational plan.
        if (reinforcementUnits < requiredReinforcements) {
            return Optional.of(new RepairPlan(target, durability, repairable, rarity, 0,
                    false, RepairPlan.Status.INSUFFICIENT_CATALYST, requiredSwings,
                    requiredReinforcements, Collections.emptyList()));
        }

        final int budget = repairable.remainingBudget(maxDamage, lifetimeMultiplier);
        final int perExecution = (int) Math.floor(maxDamage * baseFraction);
        final int restoreAmount = Math.min(durability.getDamage(), Math.min(budget, perExecution));
        if (restoreAmount <= 0) {
            return Optional.empty();
        }

        final List<RepairPlan.ReinforcementConsumption> consumptions =
                planReinforcementConsumption(reinforcements, requiredReinforcements);

        return Optional.of(new RepairPlan(target, durability, repairable, rarity, restoreAmount,
                true, RepairPlan.Status.READY, requiredSwings, requiredReinforcements,
                consumptions));
    }

    /**
     * Number of matching-tier reinforcements consumed by one repair execution for this item.
     * Scales linearly with purity level so higher-purity items cost more to maintain — but
     * <i>only</i> when the item is attuned. Unattuned items (and items missing a
     * {@link PurityComponent} altogether) are billed as {@link ItemPurity#FRAGILE} so the
     * cost displayed at the anvil cannot leak a hidden purity roll.
     */
    private int requiredReinforcements(@NotNull ItemInstance target) {
        final ItemPurity purity = target.getComponent(PurityComponent.class)
                .filter(PurityComponent::isAttuned)
                .map(PurityComponent::getPurity)
                .orElse(ItemPurity.FRAGILE);
        return reinforcementsPerPurityOrder * (purity.getLevel() + 1);
    }

    /**
     * Distributes the required reinforcement units across the available stacks in
     * iteration order. Returned consumptions can be applied directly by {@code RepairOperation}.
     */
    private @NotNull List<RepairPlan.ReinforcementConsumption> planReinforcementConsumption(
            @NotNull List<ItemInstance> reinforcements,
            int requiredReinforcements) {
        final List<RepairPlan.ReinforcementConsumption> consumptions = new ArrayList<>();
        int remaining = requiredReinforcements;
        for (ItemInstance reinforcement : reinforcements) {
            if (remaining <= 0) {
                break;
            }
            final int available = Math.max(1, reinforcement.createItemStack().getAmount());
            final int take = Math.min(available, remaining);
            consumptions.add(new RepairPlan.ReinforcementConsumption(reinforcement, take));
            remaining -= take;
        }
        return List.copyOf(consumptions);
    }
}
