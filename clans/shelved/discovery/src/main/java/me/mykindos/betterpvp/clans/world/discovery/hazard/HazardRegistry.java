package me.mykindos.betterpvp.clans.world.discovery.hazard;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The {@link HazardArchetype}s the sea can draw from, claimed by name.
 * <p>
 * The built-ins are put here by {@link HazardService}, which is what has the collaborators they need; anything else
 * that wants a hazard of its own registers the same way. Registering a name twice replaces the first, so this survives
 * a reload.
 */
@Singleton
public class HazardRegistry {

    /** Insertion-ordered so a uniform draw over {@link #all()} is reproducible rather than hash-order. */
    private final Map<String, HazardArchetype> archetypes = new LinkedHashMap<>();

    public void register(@NotNull HazardArchetype archetype) {
        archetypes.put(archetype.key().toLowerCase(Locale.ROOT), archetype);
    }

    @Nullable
    public HazardArchetype get(@NotNull String key) {
        return archetypes.get(key.toLowerCase(Locale.ROOT));
    }

    /** The claimed names, for validation messages that can say what was meant. */
    public @NotNull Set<String> keys() {
        return Set.copyOf(archetypes.keySet());
    }

    /** Everything registered, for picking one at random. */
    public @NotNull List<HazardArchetype> all() {
        return new ArrayList<>(archetypes.values());
    }
}
