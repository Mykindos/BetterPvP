package me.mykindos.betterpvp.core.loot.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.loot.AwardStrategy;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.PityRule;
import me.mykindos.betterpvp.core.loot.ProgressiveWeightConfig;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.loot.RollCountFunction;
import me.mykindos.betterpvp.core.loot.WeightDistributionStrategy;
import me.mykindos.betterpvp.core.loot.chest.BigLootChest;
import me.mykindos.betterpvp.core.loot.chest.LootChest;
import me.mykindos.betterpvp.core.loot.chest.SmallLootChest;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deserializes loot tables from JSON format.
 * <p>
 * Loot entry types are resolved via {@link LootEntryRegistry}. The built-in types
 * ("dropped_item", "given_item") are registered statically. Additional types can be
 * registered by other modules in their {@code onLoad()} method.
 */
@CustomLog
public class LootTableDeserializer implements JsonDeserializer<LootTable> {

    @Override
    public LootTable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        String id = obj.get("name").getAsString();
        ReplacementStrategy replacementStrategy = ReplacementStrategy.valueOf(obj.get("replacementStrategy").getAsString());

        // Parse rollStrategy
        RollCountFunction rollCountFunction = parseRollStrategy(obj.get("rollStrategy").getAsJsonObject());

        // Parse awardStrategy
        AwardStrategy awardStrategy = AwardStrategy.DEFAULT;
        if (obj.has("awardStrategy")) {
            awardStrategy = parseAwardStrategy(obj.get("awardStrategy").getAsJsonObject());
        }

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
            Optional<Loot<?, ?>> loot = parseLootEntry(entryObj);
            if (loot.isEmpty()) {
                continue;
            }

            lootById.put(entryId, loot.get());
            weightedLoot.put(weight, loot.get());
        }

        // Parse guaranteed entries
        List<Loot<?, ?>> guaranteedLoot = new ArrayList<>();
        if (obj.has("guaranteed")) {
            JsonArray guaranteedArray = obj.getAsJsonArray("guaranteed");
            for (JsonElement guaranteedElement : guaranteedArray) {
                JsonObject guaranteedObj = guaranteedElement.getAsJsonObject();
                Optional<Loot<?, ?>> loot = parseLootEntry(guaranteedObj);
                if (loot.isEmpty()) {
                    continue;
                }
                guaranteedLoot.add(loot.get());
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
                .awardStrategy(awardStrategy)
                .replacementStrategy(replacementStrategy)
                .rollCountFunction(rollCountFunction)
                .weightedLoot(weightedLoot)
                .guaranteedLoot(guaranteedLoot)
                .pityRules(pityRules)
                .weightDistributionStrategy(weightDistributionStrategy)
                .progressiveWeightConfig(progressiveWeightConfig)
                .build();
    }

    private @NotNull SoundEffect parseSoundEffect(@NotNull JsonObject soundEffectObj) {
        final String key = soundEffectObj.get("key").getAsString();
        final float pitch = soundEffectObj.get("pitch").getAsFloat();
        final float volume = soundEffectObj.get("volume").getAsFloat();
        Preconditions.checkArgument(pitch >= 0.0F && pitch <= 2.0F, "Invalid pitch: " + pitch);
        final NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        Preconditions.checkNotNull(namespacedKey, "Invalid sound effect key: " + key);
        return new SoundEffect(Sound.sound(namespacedKey, Sound.Source.AMBIENT, volume, pitch));
    }

    private @NotNull AwardStrategy parseAwardStrategy(@NotNull JsonObject awardStrategyObj) {
        return switch (awardStrategyObj.get("type").getAsString()) {
            case "DEFAULT": {
                yield AwardStrategy.DEFAULT;
            }
            case "LOOT_CHEST": {
                final String chestType = awardStrategyObj.get("chestType").getAsString();
                yield switch (chestType) {
                    case "SMALL": {
                        yield new SmallLootChest();
                    }
                    case "BIG": {
                        yield new BigLootChest();
                    }
                    case "CUSTOM": {
                        final String mythicMobName = awardStrategyObj.get("mythicMobName").getAsString();
                        final long dropDelay = awardStrategyObj.get("dropDelay").getAsLong();
                        final long dropInterval = awardStrategyObj.get("dropInterval").getAsLong();
                        final SoundEffect dropSound = parseSoundEffect(awardStrategyObj.get("dropSound").getAsJsonObject());
                        yield new LootChest(mythicMobName, dropSound, dropDelay, dropInterval);
                    }
                    default: {
                        throw new JsonParseException("Unknown loot chest type: " + chestType);
                    }
                };
            }
            default: {
                throw new JsonParseException("Unknown award strategy type: " + awardStrategyObj.get("type").getAsString());
            }
        };
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

    private Optional<Loot<?, ?>> parseLootEntry(@NotNull JsonObject entryObj) {
        String type = entryObj.get("type").getAsString();
        ReplacementStrategy replacementStrategy = ReplacementStrategy.UNSET;
        if (entryObj.has("replacementStrategy")) {
            replacementStrategy = ReplacementStrategy.valueOf(entryObj.get("replacementStrategy").getAsString());
        }

        Optional<LootEntryParser> parser = LootEntryRegistry.get(type);
        if (parser.isEmpty()) {
            log.warn("Unknown loot entry type '{}' - skipping entry. Did you register it in onLoad()?", type);
            return Optional.empty();
        }
        return Optional.of(parser.get().parse(entryObj, replacementStrategy));
    }
}
