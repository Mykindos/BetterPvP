package me.mykindos.betterpvp.core.loot.serialization;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.loot.*;
import me.mykindos.betterpvp.core.loot.item.ItemLoot;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Deserializes loot tables from JSON format.
 */
public class LootTableDeserializer implements JsonDeserializer<LootTable> {

    private final ItemFactory itemFactory;

    public LootTableDeserializer(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @Override
    public LootTable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        String id = obj.get("name").getAsString();
        ReplacementStrategy replacementStrategy = ReplacementStrategy.valueOf(obj.get("replacementStrategy").getAsString());

        // Parse rollStrategy
        RollCountFunction rollCountFunction = parseRollStrategy(obj.get("rollStrategy").getAsJsonObject());

        // Parse weightDistribution
        WeightDistributionStrategy weightDistributionStrategy = WeightDistributionStrategy.STATIC;
        if (obj.has("weightDistribution")) {
            weightDistributionStrategy = WeightDistributionStrategy.valueOf(obj.get("weightDistribution").getAsString());
        }

        // Parse progressive weight config if present
        ProgressiveWeightConfig progressiveWeightConfig = ProgressiveWeightConfig.builder().build();
        if (obj.has("progressiveWeightConfig")) {
            progressiveWeightConfig = context.deserialize(obj.get("progressiveWeightConfig"), ProgressiveWeightConfig.class);
        }

        // Parse entries
        JsonArray entriesArray = obj.getAsJsonArray("entries");
        Map<String, Loot<?, ?>> lootById = new HashMap<>();
        Multimap<Integer, Loot<?, ?>> weightedLoot = ArrayListMultimap.create();

        for (JsonElement entryElement : entriesArray) {
            JsonObject entryObj = entryElement.getAsJsonObject();
            String entryId = entryObj.get("id").getAsString();
            int weight = entryObj.get("weight").getAsInt();
            Loot<?, ?> loot = parseLootEntry(entryObj);

            lootById.put(entryId, loot);
            weightedLoot.put(weight, loot);
        }

        // Parse guaranteed entries
        List<Loot<?, ?>> guaranteedLoot = new ArrayList<>();
        if (obj.has("guaranteed")) {
            JsonArray guaranteedArray = obj.getAsJsonArray("guaranteed");
            for (JsonElement guaranteedElement : guaranteedArray) {
                JsonObject guaranteedObj = guaranteedElement.getAsJsonObject();
                Loot<?, ?> loot = parseLootEntry(guaranteedObj);
                guaranteedLoot.add(loot);
            }
        }

        // Parse pity rules
        List<PityRule> pityRules = new ArrayList<>();
        if (obj.has("pityRules")) {
            JsonArray pityRulesArray = obj.getAsJsonArray("pityRules");
            for (JsonElement pityRuleElement : pityRulesArray) {
                JsonObject pityRuleObj = pityRuleElement.getAsJsonObject();
                String entryId = pityRuleObj.get("entryId").getAsString();
                int maxAttempts = pityRuleObj.get("maxAttempts").getAsInt();
                int weightIncrement = pityRuleObj.get("weightIncrement").getAsInt();

                Loot<?, ?> loot = lootById.get(entryId);
                if (loot != null) {
                    PityRule pityRule = PityRule.builder()
                            .loot(loot)
                            .maxAttempts(maxAttempts)
                            .weightIncrement(weightIncrement)
                            .build();
                    pityRules.add(pityRule);
                }
            }
        }

        return LootTable.builder()
                .id(id)
                .replacementStrategy(replacementStrategy)
                .rollCountFunction(rollCountFunction)
                .weightedLoot(weightedLoot)
                .guaranteedLoot(guaranteedLoot)
                .pityRules(pityRules)
                .weightDistributionStrategy(weightDistributionStrategy)
                .progressiveWeightConfig(progressiveWeightConfig)
                .build();
    }

    private @NotNull RollCountFunction parseRollStrategy(@NotNull JsonObject rollStrategyObj) {
        String type = rollStrategyObj.get("type").getAsString();

        return switch (type) {
            case "CONSTANT" -> {
                int count = rollStrategyObj.get("rolls").getAsInt();
                yield RollCountFunction.constant(count);
            }
            case "RANDOM" -> {
                int min = rollStrategyObj.get("min").getAsInt();
                int max = rollStrategyObj.get("max").getAsInt();
                yield RollCountFunction.random(min, max);
            }
            case "PROGRESSIVE" -> {
                int baseRolls = rollStrategyObj.get("baseRolls").getAsInt();
                int incrementPerProgress = rollStrategyObj.get("rollIncrement").getAsInt();
                int maxRolls = rollStrategyObj.get("maxRolls").getAsInt();
                yield RollCountFunction.progressive(baseRolls, incrementPerProgress, maxRolls);
            }
            default -> throw new JsonParseException("Unknown roll strategy type: " + type);
        };
    }

    private @NotNull Loot<?, ?> parseLootEntry(@NotNull JsonObject entryObj) {
        String type = entryObj.get("type").getAsString();
        ReplacementStrategy replacementStrategy = ReplacementStrategy.UNSET;
        if (entryObj.has("replacementStrategy")) {
            replacementStrategy = ReplacementStrategy.valueOf(entryObj.get("replacementStrategy").getAsString());
        }

        return switch (type) {
            case "dropped_item" -> {
                String itemId = entryObj.get("itemId").getAsString();
                int minYield = entryObj.get("minYield").getAsInt();
                int maxYield = entryObj.get("maxYield").getAsInt();

                BaseItem baseItem = getBaseItem(itemId);
                yield ItemLoot.dropped(baseItem, replacementStrategy, minYield, maxYield);
            }
            case "given_item" -> {
                String itemId = entryObj.get("itemId").getAsString();
                int minYield = entryObj.get("minYield").getAsInt();
                int maxYield = entryObj.get("maxYield").getAsInt();

                BaseItem baseItem = getBaseItem(itemId);
                yield ItemLoot.given(baseItem, replacementStrategy, minYield, maxYield);
            }
            default -> throw new JsonParseException("Unknown loot entry type: " + type);
        };
    }

    private @NotNull BaseItem getBaseItem(@NotNull String itemId) {
        NamespacedKey key = NamespacedKey.fromString(itemId);
        if (key == null) {
            throw new JsonParseException("Invalid item id: " + itemId);
        }

        BaseItem baseItem = itemFactory.getItemRegistry().getItem(key);
        if (baseItem == null) {
            throw new JsonParseException("Item not found: " + itemId);
        }

        return baseItem;
    }
}
