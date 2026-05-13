package me.mykindos.betterpvp.core.loot;

import com.google.common.base.Preconditions;
import lombok.Value;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the context for which a loot table is being evaluated.
 */
@Value
public class LootContext {

    /**
     * The location at which the loot is being awarded.
     */
    @NotNull Location location;

    /**
     * The time at which the loot is being awarded.
     */
    @NotNull Instant time;

    /**
     * The loot session for the player.
     */
    @NotNull LootSession session;

    @NotNull String source;

    /**
     * Caller-supplied named values made available to expression-based roll counts, weights
     * and conditions. Values may be numbers, booleans or strings.
     */
    @NotNull Map<String, Object> inputs;

    public LootContext(@NotNull LootSession session, @NotNull Location location, @NotNull String source) {
        this(session, location, source, Map.of());
    }

    public LootContext(@NotNull LootSession session, @NotNull Location location, @NotNull String source, @NotNull Map<String, Object> inputs) {
        Preconditions.checkArgument(!source.isEmpty(), "Source cannot be empty");
        this.location = location;
        this.session = session;
        this.source = source;
        this.time = Instant.now();
        this.inputs = Map.copyOf(inputs);
    }

    /**
     * Returns a copy of this context with the given input added.
     */
    public @NotNull LootContext withInput(@NotNull String key, @Nullable Object value) {
        final Map<String, Object> merged = new HashMap<>(this.inputs);
        if (value == null) merged.remove(key);
        else merged.put(key, value);
        return new LootContext(session, location, source, merged);
    }

    /**
     * Returns the named input value, or {@code null} if absent.
     */
    public @Nullable Object getInput(@NotNull String key) {
        return inputs.get(key);
    }
}
