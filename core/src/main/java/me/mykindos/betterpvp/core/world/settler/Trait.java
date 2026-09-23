package me.mykindos.betterpvp.core.world.settler;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Something that sets one settler apart. A trait says which settlers can roll it, and what it does is read by
 * whichever system it affects, at a strength its settler's rarity decides.
 */
@Value
@Builder
public class Trait {

    @NotNull String id;
    /** Translation key prefix: its name is under {@code <key>.name} and what it does under {@code <key>.description}. */
    @NotNull String key;
    @NotNull TraitGroup group;
    /** Whether it cuts both ways, which rarer settlers roll less often. */
    boolean tradeOff;
    /** The least rare settler that can roll it. */
    @Builder.Default
    @NotNull SettlerRarity minimumRarity = SettlerRarity.COMMON;
    /** The professions that can roll it, or empty for any settler including one with no profession. */
    @Singular
    @NotNull Set<String> professions;

    public boolean canRoll(@NotNull SettlerRarity rarity, @Nullable String profession) {
        return rarity.isAtLeast(minimumRarity) && (professions.isEmpty() || professions.contains(profession));
    }

    public @NotNull String nameKey() {
        return key + ".name";
    }

    public @NotNull String descriptionKey() {
        return key + ".description";
    }
}
