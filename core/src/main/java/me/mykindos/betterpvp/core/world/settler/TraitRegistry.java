package me.mykindos.betterpvp.core.world.settler;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Every trait a settler can roll, by id. */
@Singleton
public class TraitRegistry {

    private final Map<String, Trait> traits = new LinkedHashMap<>();

    public void register(@NotNull Trait trait) {
        traits.put(trait.getId(), trait);
    }

    public @NotNull Optional<Trait> find(@NotNull String id) {
        return Optional.ofNullable(traits.get(id));
    }

    public @NotNull Collection<Trait> all() {
        return Collections.unmodifiableCollection(traits.values());
    }
}
