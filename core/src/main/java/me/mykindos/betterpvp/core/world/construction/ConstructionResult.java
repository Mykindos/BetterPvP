package me.mykindos.betterpvp.core.world.construction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** How a construction action went: done, or refused with a reason to show the player. */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstructionResult {

    boolean success;
    @Nullable Component reason;
    @Nullable PlacedStructure structure;

    public static @NotNull ConstructionResult done(@NotNull PlacedStructure structure) {
        return new ConstructionResult(true, null, structure);
    }

    public static @NotNull ConstructionResult refused(@NotNull Component reason) {
        return new ConstructionResult(false, reason, null);
    }

    public static @NotNull ConstructionResult refused(@NotNull String reason) {
        return refused(Component.text(reason, NamedTextColor.RED));
    }
}
