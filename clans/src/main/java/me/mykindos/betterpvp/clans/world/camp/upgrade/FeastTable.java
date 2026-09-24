package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.settler.morale.FoodSource;
import me.mykindos.betterpvp.core.world.settler.morale.MoraleEngine;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Great Hall upgrade: a member lays out a feast from the food in their inventory, and while it lasts every settler in
 * the camp gets its morale as food. Each item is worth the points listed under {@code food}, and a feast takes food
 * worth {@code cost} points. When the feast ends is kept on the camp record.
 */
@BPvPListener
@Singleton
public class FeastTable implements Listener, FoodSource {

    public static final String ID = "feast_table";

    private final CampUpgrades upgrades;
    private final CampConfig config;
    private final CampStore store;
    private final MoraleEngine moraleEngine;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final LongSupplier clock;

    @Inject
    public FeastTable(@NotNull CampUpgrades upgrades, @NotNull CampConfig config, @NotNull CampStore store,
                      @NotNull MoraleEngine moraleEngine, @NotNull ItemFactory itemFactory,
                      @NotNull ItemRegistry itemRegistry) {
        this(upgrades, config, store, moraleEngine, itemFactory, itemRegistry, System::currentTimeMillis);
    }

    FeastTable(@NotNull CampUpgrades upgrades, @NotNull CampConfig config, @NotNull CampStore store,
               @NotNull MoraleEngine moraleEngine, @NotNull ItemFactory itemFactory,
               @NotNull ItemRegistry itemRegistry, @NotNull LongSupplier clock) {
        this.upgrades = upgrades;
        this.config = config;
        this.store = store;
        this.moraleEngine = moraleEngine;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        this.clock = clock;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 1);
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampConstruction.GREAT_HALL, ID);
    }

    @Override
    public int morale(@NotNull SiteKey site) {
        return isActive(site) && timeLeft(site) > 0 ? bonus() : 0;
    }

    /** How long camp {@code key}'s feast has left, or 0 when none is laid. */
    public long timeLeft(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId())
                .map(camp -> Math.max(0, camp.getFeastUntil() - clock.getAsLong()))
                .orElse(0L);
    }

    /** The morale a feast gives every settler while it lasts. */
    public int bonus() {
        return numbers().map(numbers -> numbers.setting("morale", 15)).orElse(15);
    }

    /** The food points one feast takes. */
    public int cost() {
        return Math.max(1, numbers().map(numbers -> numbers.setting("cost", 320)).orElse(320));
    }

    /** How long one feast lasts. */
    public @NotNull Duration length() {
        final double hours = numbers().map(numbers -> numbers.setting("hours", 24.0)).orElse(24.0);
        return Duration.ofMinutes(Math.round(hours * 60));
    }

    /** What each item is worth toward a feast, by item key. */
    public @NotNull Map<String, Integer> values() {
        final Map<String, Integer> values = new LinkedHashMap<>();
        numbers().map(numbers -> numbers.amounts("food")).orElse(Map.of())
                .forEach((item, points) -> values.put(item.toLowerCase(Locale.ROOT), points));
        return values;
    }

    /** The food points {@code player} carries. */
    public int carried(@NotNull Player player) {
        return pantry(player.getInventory()).values().stream()
                .mapToInt(portion -> portion.getPoints() * portion.getAmount())
                .sum();
    }

    /**
     * Lays a feast in camp {@code key} with food from {@code player}'s inventory.
     *
     * @return the translation key of why it was refused, or null once laid
     */
    public @Nullable String lay(@NotNull Player player, @NotNull SiteKey key) {
        final String refused = refusal(key);
        if (refused != null) {
            return refused;
        }
        final PlayerInventory inventory = player.getInventory();
        final Map<Integer, Integer> taking = take(pantry(inventory), cost());
        if (taking.isEmpty()) {
            return "clans.camp.upgrade.feast_table.not_enough";
        }
        taking.forEach((slot, amount) -> {
            final ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getAmount() <= amount) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - amount);
                inventory.setItem(slot, stack);
            }
        });
        begin(key);
        return null;
    }

    /** Why a feast could not be laid in camp {@code key} now, or null if it could. */
    @Nullable String refusal(@NotNull SiteKey key) {
        if (store.cached(key.getOwnerId()).isEmpty()) {
            return "core.settler.not_loaded";
        }
        if (!isActive(key)) {
            return "clans.camp.upgrade.feast_table.inactive";
        }
        if (timeLeft(key) > 0) {
            return "clans.camp.upgrade.feast_table.already";
        }
        return null;
    }

    /** Starts a feast in camp {@code key} and lifts its settlers straight away. */
    void begin(@NotNull SiteKey key) {
        store.cached(key.getOwnerId()).ifPresent(camp -> {
            camp.setFeastUntil(clock.getAsLong() + length().toMillis());
            store.changed(key.getOwnerId());
            moraleEngine.settle(key);
        });
    }

    /**
     * How many to take from each slot for food worth {@code cost} points, going through the slots in order and
     * taking no more of the last than it needs. Empty when everything together is worth less.
     */
    static @NotNull Map<Integer, Integer> take(@NotNull Map<Integer, Portion> food, int cost) {
        final Map<Integer, Integer> taking = new LinkedHashMap<>();
        int remaining = cost;
        for (Map.Entry<Integer, Portion> entry : food.entrySet()) {
            if (remaining <= 0) {
                break;
            }
            final Portion portion = entry.getValue();
            if (portion.getPoints() <= 0 || portion.getAmount() <= 0) {
                continue;
            }
            final int needed = (remaining + portion.getPoints() - 1) / portion.getPoints();
            final int amount = Math.min(portion.getAmount(), needed);
            taking.put(entry.getKey(), amount);
            remaining -= amount * portion.getPoints();
        }
        return remaining > 0 ? Map.of() : taking;
    }

    /** The food in {@code inventory}, by slot. */
    private @NotNull Map<Integer, Portion> pantry(@NotNull PlayerInventory inventory) {
        final Map<String, Integer> values = values();
        final ItemStack[] contents = inventory.getStorageContents();
        final Map<Integer, Portion> food = new LinkedHashMap<>();
        for (int slot = 0; slot < contents.length; slot++) {
            final ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            final Integer points = values.get(itemKey(stack));
            if (points != null && points > 0) {
                food.put(slot, new Portion(points, stack.getAmount()));
            }
        }
        return food;
    }

    /** A custom item's own key, or {@code minecraft:<material>}. */
    private @NotNull String itemKey(@NotNull ItemStack stack) {
        return itemFactory.fromItemStack(stack)
                .map(instance -> itemRegistry.getKey(instance.getBaseItem()))
                .map(NamespacedKey::toString)
                .orElseGet(() -> NamespacedKey.minecraft(stack.getType().name().toLowerCase(Locale.ROOT)).toString());
    }

    private @NotNull Optional<CampConfig.UpgradeNumbers> numbers() {
        return config.upgrade(CampConstruction.GREAT_HALL, ID);
    }

    /** One stack of food: what each item is worth, and how many there are. */
    @Value
    static class Portion {
        int points;
        int amount;
    }
}
