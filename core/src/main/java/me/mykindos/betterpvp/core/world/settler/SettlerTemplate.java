package me.mykindos.betterpvp.core.world.settler;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** What a new settler must be, with everything else left to the roll. */
@Value
@Builder
public class SettlerTemplate {

    @NotNull SettlerRarity rarity;
    /** Its profession id, or null for a settler with none. */
    @Nullable String profession;
    /** Where it came from, which picks its history line. */
    @NotNull String source;
    /** What its history line fills in, such as the name of the dungeon it was freed from. */
    @Singular
    @NotNull List<String> historyArgs;
}
