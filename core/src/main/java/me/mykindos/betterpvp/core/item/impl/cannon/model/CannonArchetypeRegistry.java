package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.impl.cannon.firing.PassengerFiringMode;
import me.mykindos.betterpvp.core.item.impl.cannon.firing.ProjectileFiringMode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The kinds of cannon this server knows about.
 * <p>
 * Built-ins are declared here rather than discovered by reflection because an archetype is a configuration value, not
 * a behaviour: several archetypes can share a {@link me.mykindos.betterpvp.core.item.impl.cannon.firing.CannonFiringMode}
 * and differ only in their timings. Additional archetypes can be registered by any module via {@link #register}.
 */
@Singleton
@CustomLog
public class CannonArchetypeRegistry {

    /** The archetype used when nothing else is specified. */
    public static final String STANDARD = "standard";

    /** The human cannonball. */
    public static final String LAUNCHER = "launcher";

    private final Map<String, CannonArchetype> byId = new LinkedHashMap<>();

    @Inject
    private CannonArchetypeRegistry(CannonConfig config, ProjectileFiringMode projectile, PassengerFiringMode passenger) {
        register(CannonArchetype.builder()
                .id(STANDARD)
                .firingMode(projectile)
                .fuse(config::getFuseSeconds)
                .cooldown(config::getShootCooldownSeconds)
                .build());

        // Same model and animations as the standard cannon - only the timings and what leaves the barrel differ.
        register(CannonArchetype.builder()
                .id(LAUNCHER)
                .firingMode(passenger)
                .fuse(config::getLauncherFuseSeconds)
                .cooldown(config::getLauncherCooldownSeconds)
                .build());

        log.info("Loaded {} cannon archetype(s): {}", byId.size(), byId.keySet()).submit();
    }

    public void register(@NotNull CannonArchetype archetype) {
        byId.put(archetype.getId().toLowerCase(Locale.ROOT), archetype);
    }

    public @NotNull Optional<CannonArchetype> get(@NotNull String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public @NotNull Collection<CannonArchetype> all() {
        return byId.values();
    }

    public @NotNull String[] ids() {
        return byId.keySet().toArray(new String[0]);
    }
}
