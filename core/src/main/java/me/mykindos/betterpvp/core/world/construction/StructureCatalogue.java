package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Every structure type that can be built, by id. */
@Singleton
public class StructureCatalogue {

    private final Map<String, StructureType> types = new LinkedHashMap<>();

    public void register(@NotNull StructureType type) {
        types.put(type.getId(), type);
    }

    public @NotNull Optional<StructureType> find(@NotNull String id) {
        return Optional.ofNullable(types.get(id));
    }

    public @NotNull Collection<StructureType> all() {
        return Collections.unmodifiableCollection(types.values());
    }
}
