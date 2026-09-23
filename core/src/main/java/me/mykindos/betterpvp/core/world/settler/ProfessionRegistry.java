package me.mykindos.betterpvp.core.world.settler;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Every profession a settler can have, by id. */
@Singleton
public class ProfessionRegistry {

    private final Map<String, Profession> professions = new LinkedHashMap<>();

    public void register(@NotNull Profession profession) {
        professions.put(profession.getId(), profession);
    }

    public @NotNull Optional<Profession> find(@NotNull String id) {
        return Optional.ofNullable(professions.get(id));
    }

    public @NotNull Collection<Profession> all() {
        return Collections.unmodifiableCollection(professions.values());
    }
}
