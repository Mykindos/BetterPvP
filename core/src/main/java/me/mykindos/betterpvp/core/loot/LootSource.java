package me.mykindos.betterpvp.core.loot;

import com.google.common.base.Preconditions;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies the originating flow of a {@link LootContext}.
 * <p>
 * The {@code displayName} is the human-facing label (e.g. "Fishing") and may collide across
 * distinct flows. The {@code id} is the unique, machine-facing identifier (e.g.
 * "fishing:catch" vs. "fishing:treasure") that listeners filter on to differentiate flows
 * that share a display name.
 */
@Value
public class LootSource {

    @NotNull String displayName;
    @NotNull String id;

    public LootSource(@NotNull String displayName, @NotNull String id) {
        Preconditions.checkArgument(!displayName.isEmpty(), "displayName cannot be empty");
        Preconditions.checkArgument(!id.isEmpty(), "id cannot be empty");
        this.displayName = displayName;
        this.id = id;
    }

    public static @NotNull LootSource of(@NotNull String displayName, @NotNull String id) {
        return new LootSource(displayName, id);
    }
}
