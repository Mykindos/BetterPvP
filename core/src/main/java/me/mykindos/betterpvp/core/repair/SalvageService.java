package me.mykindos.betterpvp.core.repair;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.ReinforcementComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.RepairableComponent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves whether a held {@link ItemStack} is salvageable at a Salvage Station, and
 * picks the matching-tier reinforcement that the salvage will yield.
 * <br>
 * Salvageability mirrors the repair contract: the item must carry both a
 * {@link DurabilityComponent} and a {@link RepairableComponent} (i.e. it's gear that
 * could have been repaired in the first place) and be non-stackable (max stack size 1)
 * so that a single right-click unambiguously consumes a single unique item.
 * <br>
 * The reinforcement is chosen by scanning the {@link ItemRegistry} for a BaseItem whose
 * {@link ReinforcementComponent#getTier()} equals the salvaged item's runtime rarity;
 * the mapping is cached on first use because {@link ItemRegistry} is populated
 * asynchronously at startup and may be empty if we eagerly resolved.
 */
@Singleton
public class SalvageService {

    private final ItemRegistry itemRegistry;
    private volatile Map<ItemRarity, BaseItem> reinforcementByTier;

    /** Probability that a salvage yields the reinforcement of the next rarity tier instead of the item's own tier. */
    @Inject
    @Config(path = "salvage.tierUpgradeChance", defaultValue = "0.20")
    private double tierUpgradeChance;

    /** Minimum yield, awarded when the item is fully broken (durability fraction = 0). */
    @Inject
    @Config(path = "salvage.minYield", defaultValue = "1")
    private int minYield;

    /** Maximum durability-driven yield, awarded when the item is pristine (durability fraction = 1). Purity bonuses stack on top. */
    @Inject
    @Config(path = "salvage.maxYield", defaultValue = "3")
    private int maxYield;

    /** Extra reinforcements awarded for an attuned item of PITIFUL or FRAGILE purity. */
    @Inject
    @Config(path = "salvage.purityBonus.low", defaultValue = "0")
    private int purityBonusLow;

    /** Extra reinforcements awarded for an attuned item of MODERATE or POLISHED purity. */
    @Inject
    @Config(path = "salvage.purityBonus.mid", defaultValue = "1")
    private int purityBonusMid;

    /** Extra reinforcements awarded for an attuned item of PRISTINE or PERFECT purity. */
    @Inject
    @Config(path = "salvage.purityBonus.high", defaultValue = "2")
    private int purityBonusHigh;

    @Inject
    private SalvageService(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /**
     * Resolves a salvage plan from the player's held {@link ItemInstance}.
     *
     * @return the plan, or empty if the item is not eligible (not gear, stackable, or
     * its tier has no registered reinforcement).
     */
    public @NotNull Optional<SalvagePlan> resolve(@NotNull ItemInstance instance, @NotNull ItemStack heldStack) {
        if (heldStack.getMaxStackSize() > 1) {
            return Optional.empty();
        }
        final boolean isGear = instance.getComponent(DurabilityComponent.class).isPresent()
                && instance.getComponent(RepairableComponent.class).isPresent();
        if (!isGear) {
            return Optional.empty();
        }

        final ItemRarity rarity = instance.getRarity();
        final BaseItem baseReinforcement = reinforcementFor(rarity);
        if (baseReinforcement == null) {
            return Optional.empty();
        }

        final DurabilityComponent durability = instance.getComponent(DurabilityComponent.class).orElseThrow();
        final double durabilityFraction = Math.max(0d, Math.min(1d,
                (durability.getMaxDamage() - durability.getDamage()) / (double) durability.getMaxDamage()));

        // With probability `tierUpgradeChance`, promote the yield to the next rarity's
        // reinforcement. At MYTHICAL there is no higher tier, so the roll is skipped.
        // If the upgraded tier has no registered reinforcement, fall back to the base tier.
        BaseItem finalReinforcement = baseReinforcement;
        ItemRarity finalTier = rarity;
        final ItemRarity upgradedTier = nextTier(rarity);
        if (upgradedTier != null && ThreadLocalRandom.current().nextDouble() < tierUpgradeChance) {
            final BaseItem upgraded = reinforcementFor(upgradedTier);
            if (upgraded != null) {
                finalReinforcement = upgraded;
                finalTier = upgradedTier;
            }
        }

        final int baseYield = rollYield(durabilityFraction);
        final int yield = baseYield + purityBonus(instance);
        return Optional.of(new SalvagePlan(instance, finalReinforcement, finalTier, yield));
    }

    /**
     * Maps remaining-durability fraction in [0, 1] to a salvage yield in [minYield, maxYield].
     * Higher durability should bias toward higher yields.
     *
     * @param durabilityFraction (maxDamage - damage) / maxDamage, clamped to [0, 1].
     *                           1.0 = pristine, 0.0 = broken.
     */
    private int rollYield(double durabilityFraction) {
        return (int) Math.round(minYield + durabilityFraction * (maxYield - minYield));
    }

    /**
     * Returns the extra reinforcements awarded by the item's purity, or 0 if the item
     * has no {@link PurityComponent} or is unattuned. Bonuses are grouped in pairs along
     * the {@link ItemPurity} scale and are configurable via {@code salvage.purityBonus.*}.
     */
    private int purityBonus(@NotNull ItemInstance instance) {
        final PurityComponent component = instance.getComponent(PurityComponent.class).orElse(null);
        if (component == null || !component.isAttuned()) {
            return 0;
        }
        return switch (component.getPurity()) {
            case PITIFUL, FRAGILE -> purityBonusLow;
            case MODERATE, POLISHED -> purityBonusMid;
            case PRISTINE, PERFECT -> purityBonusHigh;
        };
    }

    /** Returns the rarity one tier above {@code rarity}, or null if it is already the top tier. */
    private @Nullable ItemRarity nextTier(@NotNull ItemRarity rarity) {
        final ItemRarity[] all = ItemRarity.values();
        final int next = rarity.ordinal() + 1;
        return next < all.length ? all[next] : null;
    }

    /**
     * Finds the registered Reinforcement BaseItem for the given rarity tier, building
     * the cache on first call. Returns null if no reinforcement exists for that tier
     * (which shouldn't happen for current rarities — all six are registered today).
     */
    private BaseItem reinforcementFor(@NotNull ItemRarity rarity) {
        Map<ItemRarity, BaseItem> cache = reinforcementByTier;
        if (cache == null) {
            cache = buildCache();
            reinforcementByTier = cache;
        }
        return cache.get(rarity);
    }

    private @NotNull Map<ItemRarity, BaseItem> buildCache() {
        final Map<ItemRarity, BaseItem> built = new EnumMap<>(ItemRarity.class);
        for (BaseItem item : itemRegistry.getItems().values()) {
            item.getComponent(ReinforcementComponent.class)
                    .ifPresent(component -> built.putIfAbsent(component.getTier(), item));
        }
        return built;
    }
}
