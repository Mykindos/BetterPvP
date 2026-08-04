package me.mykindos.betterpvp.clans.world.discovery.hazard;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.discovery.Expedition;
import me.mykindos.betterpvp.clans.world.discovery.ExpeditionService;
import me.mykindos.betterpvp.clans.world.discovery.HazardSpawnPolicy;
import me.mykindos.betterpvp.clans.world.discovery.Ocean;
import me.mykindos.betterpvp.core.effects.EffectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The hazards on every running expedition: rolling for them, drifting them past the hull, and paying for the ones that
 * were not steered around.
 * <p>
 * A field is attached on the first tick that asks for one and released when the crew comes about, exactly as the
 * steering controls are — an expedition that has ended must not leave a mound of ice in a world about to be recycled.
 */
@Singleton
public class HazardService {

    private final HazardRegistry registry;
    private final HazardConfig config;

    private final Map<UUID, HazardField> fields = new ConcurrentHashMap<>();

    @Inject
    public HazardService(@NotNull HazardRegistry registry, @NotNull HazardConfig config,
                         @NotNull EffectManager effects, @NotNull Provider<ExpeditionService> expeditions) {
        this.registry = registry;
        this.config = config;

        registry.register(new Whirlpool.Archetype(config, this));
        registry.register(new Iceberg.Archetype(config, effects, expeditions));
    }

    /**
     * The input the ship actually gets this tick.
     * <p>
     * Whatever has hold of the wheel wins over the crew's own controls, which is what makes a whirlpool something to be
     * ridden out rather than something to be steered against.
     */
    public int steer(@NotNull Expedition expedition, int crewInput, long now) {
        return field(expedition).getOverride().apply(crewInput, now);
    }

    /** The wheel, for a hazard that wants to take it. */
    public @NotNull SteeringOverride override(@NotNull Expedition expedition) {
        return field(expedition).getOverride();
    }

    /**
     * Rolls for a new hazard, moves everything already out there, and pays for anything the hull has run into.
     *
     * @param dt seconds elapsed, the same step the ship was advanced by
     */
    public void tick(@NotNull Expedition expedition, double dt, long now) {
        final HazardField field = field(expedition);
        final Ocean ocean = expedition.getOcean();
        final double heading = expedition.getDynamics().getHeading();

        // Depth stays at zero: the policy keeps the knob so hazard density can be made to climb later, and nothing
        // drives it today.
        new HazardSpawnPolicy(0.3, 100, 25, 75, 0.3, 0.15).next(0, dt, ThreadLocalRandom.current()::nextDouble).ifPresent(spawn -> {
            final HazardArchetype archetype = pick();
            if (archetype != null) {
                field.add(archetype.create(ocean.pointAt(heading, spawn.getBearing(), spawn.getDistance())));
            }
        });

        field.sweep(point -> ocean.localOf(point, heading), expedition::render,
                        expedition.getHull(), config.getDespawnRadius())
                .ifPresent(hazard -> hazard.onCollide(expedition));
    }

    /** Clears an expedition's sea, taking every hazard's entities with it. */
    public void release(@NotNull Expedition expedition) {
        final HazardField field = fields.remove(expedition.getCrew().getCaptain());
        if (field != null) {
            field.clear();
        }
    }

    private @NotNull HazardField field(@NotNull Expedition expedition) {
        return fields.computeIfAbsent(expedition.getCrew().getCaptain(), captain -> new HazardField());
    }

    /** Uniformly over what is registered. Weighting is a knob for when there is more than a pair to choose between. */
    private @Nullable HazardArchetype pick() {
        final List<HazardArchetype> archetypes = registry.all();
        return archetypes.isEmpty() ? null : archetypes.get(ThreadLocalRandom.current().nextInt(archetypes.size()));
    }
}
