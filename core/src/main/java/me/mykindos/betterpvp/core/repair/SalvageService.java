package me.mykindos.betterpvp.core.repair;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.ReinforcementComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.RepairableComponent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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

    private static final int MIN_YIELD = 1;
    private static final int MAX_YIELD = 3;

    private final ItemRegistry itemRegistry;
    private volatile Map<ItemRarity, BaseItem> reinforcementByTier;

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
        final BaseItem reinforcement = reinforcementFor(rarity);
        if (reinforcement == null) {
            return Optional.empty();
        }

        final int yield = ThreadLocalRandom.current().nextInt(MIN_YIELD, MAX_YIELD + 1);
        return Optional.of(new SalvagePlan(instance, reinforcement, rarity, yield));
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
