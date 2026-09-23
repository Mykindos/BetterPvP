package me.mykindos.betterpvp.core.world.settler;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * What new settlers are rolled from: the numbers for each rarity, the names to put together, and the history lines
 * for each place a settler can come from.
 */
@Value
public class SettlerTable {

    @NotNull Map<SettlerRarity, RarityNumbers> rarities;
    @NotNull List<String> firstNames;
    @NotNull List<String> bynames;
    /** Translation keys of history lines, by source. */
    @NotNull Map<String, List<String>> histories;

    public @NotNull RarityNumbers rarity(@NotNull SettlerRarity rarity) {
        return rarities.getOrDefault(rarity, RarityNumbers.PLAIN);
    }
}
