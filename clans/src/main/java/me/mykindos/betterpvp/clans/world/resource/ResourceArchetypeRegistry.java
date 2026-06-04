package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Discovers every concrete {@link ResourceArchetype} on the classpath (reflection over this package, mirroring how
 * {@code FieldsRepository} discovered ore types) and indexes them by {@link ResourceArchetype#id()}. Archetypes are
 * Guice-instantiated so they can inject their collaborators ({@link ResourceLoot}, schematic services, etc.).
 */
@Singleton
@CustomLog
public class ResourceArchetypeRegistry {

    private final Map<String, ResourceArchetype> byId = new HashMap<>();

    @Inject
    public ResourceArchetypeRegistry(@NotNull Clans clans) {
        final Reflections reflections = new Reflections(getClass().getPackageName());
        for (Class<? extends ResourceArchetype> clazz : reflections.getSubTypesOf(ResourceArchetype.class)) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            final ResourceArchetype archetype = clans.getInjector().getInstance(clazz);
            byId.put(archetype.id().toLowerCase(Locale.ROOT), archetype);
        }
        log.info("Loaded {} resource archetype(s): {}", byId.size(), byId.keySet()).submit();
    }

    public @NotNull Optional<ResourceArchetype> get(@NotNull String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }
}
