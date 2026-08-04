package me.mykindos.betterpvp.clans.world.props;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The {@link PropArchetype}s available to {@code type:} tags, claimed by name.
 * <p>
 * Same bargain as the interaction registry: a feature claims a name, a builder types it on a marker, and neither knows
 * about the other. Registering the same name twice replaces the first, so this survives a reload.
 */
@Singleton
public class PropArchetypeRegistry {

    private final Map<String, PropArchetype> archetypes = new HashMap<>();

    public void register(@NotNull String key, @NotNull PropArchetype archetype) {
        archetypes.put(key.toLowerCase(Locale.ROOT), archetype);
    }

    @Nullable
    public PropArchetype get(@NotNull String key) {
        return archetypes.get(key.toLowerCase(Locale.ROOT));
    }

    /** The claimed names, for validation messages that can tell a builder what they meant to type. */
    public @NotNull Set<String> keys() {
        return archetypes.keySet();
    }
}
