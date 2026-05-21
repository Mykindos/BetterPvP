package me.mykindos.betterpvp.core.block.impl.anvil.operation;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.RepairableComponent;
import me.mykindos.betterpvp.core.repair.RepairPlan;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Anvil operation that repairs a damaged item in place: restores durability on the
 * target item, consumes the planned number of tier-matching Reinforcement units, and
 * leaves the repaired item on the anvil.
 * <br>
 * Roles are resolved by item identity and type, never by slot — placement order on
 * the anvil is irrelevant. Unlike {@link CraftOperation} this produces no new item;
 * it mutates an input, which is exactly why repair is an {@link AnvilOperation} and
 * not a {@code Recipe}.
 */
@RequiredArgsConstructor
public class RepairOperation implements AnvilOperation {

    private final RepairPlan plan;
    private final ItemFactory itemFactory;

    @Override
    public int requiredSwings() {
        return plan.getRequiredSwings();
    }

    @Override
    public @NotNull Component hologramText(int currentSwings) {
        return switch (plan.getStatus()) {
            // Exhausted item: red headline + the would-be cost so the player still knows
            // which catalyst the repair would have demanded.
            case EXHAUSTED -> Component.text("Not Repairable", NamedTextColor.RED)
                    .append(Component.newline())
                    .append(reinforcementCostLine());

            // Player is missing catalyst — surface only the cost so they know what to add.
            // No swing counter: swings are no-ops in this state.
            case INSUFFICIENT_CATALYST -> reinforcementCostLine();

            case READY -> {
                final int required = requiredSwings();
                final int remaining = required - currentSwings;
                if (remaining <= 0) {
                    yield Component.empty();
                }
                final TextColor color = ProgressColor.of((float) currentSwings / required).getTextColor();
                final TextColor textColor = TextColor.color(
                        (int) (color.red() + (255 - color.red()) *  0.3f),
                        (int) (color.green() + (255 - color.green()) * 0.3f),
                        (int) (color.blue() + (255 - color.blue()) * 0.3f)
                );
                yield reinforcementCostLine()
                        .append(Component.newline())
                        .append(Component.text(remaining + "x ", color).append(Component.text("Hammer Swings", textColor)));
            }
        };
    }

    /**
     * Renders the reinforcement-cost line shown above the swing counter.
     * Format: "<count>x <Tier> Reinforcement[s]" — count in white, tier+noun in the
     * tier's rarity color so players can spot the required catalyst at a glance.
     */
    private @NotNull Component reinforcementCostLine() {
        final int count = plan.getRequiredReinforcements();
        // catalystTier is null on the non-repairable plan; fall back to the item's
        // rarity since that is the tier the repair would have demanded.
        final ItemRarity tier = plan.getCatalystTier() != null
                ? plan.getCatalystTier()
                : plan.getItem().getRarity();
        final String noun = count == 1 ? "Reinforcement" : "Reinforcements";
        return Component.text(count + "x ", NamedTextColor.YELLOW)
                .append(Component.text(tier.getName() + " " + noun, tier.getColor()));
    }

    @Override
    public @NotNull List<ItemInstance> complete(@NotNull Player player,
                                                @NotNull Map<Integer, ItemInstance> items,
                                                @NotNull Location location) {
        // Exhausted items cannot be repaired; the swing does nothing but signal failure.
        if (!plan.isCanRepair()) {
            new SoundEffect(Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.5f).play(location);
            return new ArrayList<>(items.values());
        }

        // The service decided per-stack consumption up front (see RepairService#resolve).
        // Anything not in this map — or with leftover units after a partial consumption —
        // goes back on the anvil instead of being silently destroyed.
        final Map<ItemInstance, Integer> toConsume = new HashMap<>();
        for (RepairPlan.ReinforcementConsumption consumption : plan.getConsumptions()) {
            toConsume.put(consumption.getInstance(), consumption.getUnitsConsumed());
        }

        final List<ItemInstance> remaining = new ArrayList<>();
        for (ItemInstance instance : items.values()) {
            if (instance == null) {
                continue;
            }

            if (instance == plan.getItem()) {
                applyRepair(instance);
                remaining.add(instance);
                continue;
            }

            final Integer units = toConsume.get(instance);
            if (units == null || units <= 0) {
                // Not part of the planned consumption — leave it untouched.
                remaining.add(instance);
            } else {
                consume(instance, units).ifPresent(remaining::add);
            }
        }

        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.0f, 1.2f).play(location);
        new SoundEffect(Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f).play(location);
        return remaining;
    }

    private void applyRepair(@NotNull ItemInstance target) {
        final DurabilityComponent durability = target.getComponent(DurabilityComponent.class).orElse(null);
        final RepairableComponent repairable = target.getComponent(RepairableComponent.class).orElse(null);
        if (durability == null || repairable == null) {
            return;
        }

        final int newDamage = Math.max(0, durability.getDamage() - plan.getRestoreAmount());
        final int actuallyRestored = durability.getDamage() - newDamage;
        durability.setDamage(newDamage);
        repairable.addRestored(actuallyRestored);
        target.serializeAllComponentsToItemStack();
    }

    private Optional<ItemInstance> consume(@NotNull ItemInstance source, int units) {
        final ItemStack stack = source.createItemStack();
        final int remaining = stack.getAmount() - units;
        if (remaining <= 0) {
            return Optional.empty();
        }
        stack.setAmount(remaining);
        return itemFactory.fromItemStack(stack);
    }
}
