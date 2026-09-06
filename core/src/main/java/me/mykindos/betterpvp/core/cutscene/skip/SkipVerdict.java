package me.mykindos.betterpvp.core.cutscene.skip;

import lombok.Value;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Whether a skip is allowed, and what to tell the player about it.
 * <p>
 * The hint travels with the verdict rather than being decided separately so a prompt can never be drawn for a skip that
 * would be refused - the failure mode of expressing this as a boolean.
 */
@Value
public class SkipVerdict {

    private static final SkipVerdict DENIED = new SkipVerdict(false, null);

    boolean allowed;
    @Nullable Component hint;

    public static @NotNull SkipVerdict allow(@Nullable Component hint) {
        return new SkipVerdict(true, hint);
    }

    public static @NotNull SkipVerdict deny() {
        return DENIED;
    }
}
