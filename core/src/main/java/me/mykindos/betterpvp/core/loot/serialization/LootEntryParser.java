package me.mykindos.betterpvp.core.loot.serialization;

import com.google.gson.JsonObject;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import org.jetbrains.annotations.NotNull;

/**
 * Parses a loot entry from a JSON object into a {@link Loot} instance.
 * Register implementations via {@link LootEntryRegistry#register(String, LootEntryParser)}.
 */
@FunctionalInterface
public interface LootEntryParser {

    /**
     * @param entryObj            The full JSON object for this entry.
     * @param replacementStrategy The replacement strategy already extracted from the entry.
     * @return The parsed loot instance.
     */
    Loot<?, ?> parse(@NotNull JsonObject entryObj, @NotNull ReplacementStrategy replacementStrategy);

}
