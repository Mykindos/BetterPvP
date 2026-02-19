package me.mykindos.betterpvp.core.loot.serialization;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.loot.economy.CoinLoot;
import me.mykindos.betterpvp.core.loot.item.ItemLoot;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Static registry mapping loot entry type strings to their {@link LootEntryParser}s.
 * <p>
 * Core registers built-in types ("dropped_item", "given_item") automatically.
 * Other modules should register their custom types in {@code onLoad()} to guarantee
 * they are present before loot tables are loaded during Core's {@code onEnable()}.
 * <p>
 * Example (in a module's plugin class):
 * <pre>{@code
 * @Override
 * public void onLoad() {
 *     LootEntryRegistry.register("clan_experience", (obj, strategy) -> {
 *         double amount = obj.get("amount").getAsDouble();
 *         return new ClanExperienceLoot(strategy, amount);
 *     });
 * }
 * }</pre>
 */
public final class LootEntryRegistry {

    private static final Map<String, LootEntryParser> PARSERS = new HashMap<>();

    static {
        LootEntryRegistry.register("dropped_item", (obj, strategy) -> {
            String itemId = obj.get("itemId").getAsString();
            int minYield = obj.get("minYield").getAsInt();
            int maxYield = obj.get("maxYield").getAsInt();
            NamespacedKey key = NamespacedKey.fromString(itemId);
            Preconditions.checkNotNull(key, "Invalid item ID: " + itemId);
            return ItemLoot.dropped(key, strategy, minYield, maxYield);
        });

        LootEntryRegistry.register("given_item", (obj, strategy) -> {
            String itemId = obj.get("itemId").getAsString();
            int minYield = obj.get("minYield").getAsInt();
            int maxYield = obj.get("maxYield").getAsInt();
            NamespacedKey key = NamespacedKey.fromString(itemId);
            Preconditions.checkNotNull(key, "Invalid item ID: " + itemId);
            return ItemLoot.given(key, strategy, minYield, maxYield);
        });

        LootEntryRegistry.register("given_coin", (obj, strategy) -> {
            CoinItem coinType = CoinItem.valueOf(obj.get("coinType").getAsString());
            int minAmount = obj.get("minAmount").getAsInt();
            int maxAmount = obj.get("maxAmount").getAsInt();
            return CoinLoot.given(coinType, strategy, minAmount, maxAmount);
        });

        LootEntryRegistry.register("dropped_coin", (obj, strategy) -> {
            CoinItem coinType = CoinItem.valueOf(obj.get("coinType").getAsString());
            int minAmount = obj.get("minAmount").getAsInt();
            int maxAmount = obj.get("maxAmount").getAsInt();
            return CoinLoot.dropped(coinType, strategy, minAmount, maxAmount);
        });
    }

    private LootEntryRegistry() {
    }

    /**
     * Registers a parser for the given loot entry type string.
     * Should be called in {@code onLoad()} to ensure registration before loot tables load.
     *
     * @param type   The type string as it appears in the JSON (e.g. {@code "clan_experience"}).
     * @param parser The parser that constructs the loot instance from a JSON entry object.
     * @throws IllegalArgumentException if a parser for this type is already registered.
     */
    public static void register(@NotNull String type, @NotNull LootEntryParser parser) {
        Preconditions.checkArgument(!PARSERS.containsKey(type),
                "A loot entry parser for type '%s' is already registered.", type);
        PARSERS.put(type, parser);
    }

    /**
     * Returns the parser registered for the given type, if any.
     */
    public static Optional<LootEntryParser> get(@NotNull String type) {
        return Optional.ofNullable(PARSERS.get(type));
    }

}
