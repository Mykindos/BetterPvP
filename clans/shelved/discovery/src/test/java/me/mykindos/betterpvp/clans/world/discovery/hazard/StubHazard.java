package me.mykindos.betterpvp.clans.world.discovery.hazard;

import me.mykindos.betterpvp.clans.world.discovery.Expedition;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/** A hazard that records what was asked of it, so the field's decisions can be read back without a world. */
class StubHazard extends Hazard {

    /** An archetype with a stated key and radius that hands back stubs. */
    static class StubArchetype implements HazardArchetype {

        private final String key;
        private final double radius;

        StubArchetype(@NotNull String key, double radius) {
            this.key = key;
            this.radius = radius;
        }

        @Override
        public @NotNull String key() {
            return key;
        }

        @Override
        public double radius() {
            return radius;
        }

        @Override
        public @NotNull Hazard create(@NotNull OceanPoint at) {
            return new StubHazard(this, at);
        }
    }

    private int renders;
    private int collisions;
    private int removals;
    private Location lastRender;

    StubHazard(@NotNull HazardArchetype archetype, @NotNull OceanPoint point) {
        super(archetype, point);
    }

    static @NotNull StubHazard at(double x, double z, double radius) {
        return new StubHazard(new StubArchetype("stub", radius), new OceanPoint(x, z));
    }

    int getRenders() {
        return renders;
    }

    int getCollisions() {
        return collisions;
    }

    int getRemovals() {
        return removals;
    }

    Location getLastRender() {
        return lastRender;
    }

    @Override
    public void render(@NotNull Location at) {
        renders++;
        lastRender = at;
    }

    @Override
    public void onCollide(@NotNull Expedition expedition) {
        collisions++;
    }

    @Override
    public void remove() {
        removals++;
    }
}
