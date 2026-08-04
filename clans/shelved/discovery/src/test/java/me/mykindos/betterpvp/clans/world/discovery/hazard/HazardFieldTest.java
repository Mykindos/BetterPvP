package me.mykindos.betterpvp.clans.world.discovery.hazard;

import me.mykindos.betterpvp.clans.world.discovery.HazardSpawnPolicy;
import me.mykindos.betterpvp.clans.world.discovery.OceanOffset;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import me.mykindos.betterpvp.core.config.Config;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazardFieldTest {

    private static final BoundingBox HULL = new BoundingBox(-5, 60, -15, 5, 70, 15);

    /** The plane's coordinates read straight back as a ship-local offset, so a test can place things by hand. */
    private static final Function<OceanPoint, OceanOffset> DIRECT =
            point -> new OceanOffset(point.getX(), point.getZ());

    /** A ship moored on the origin facing {@code +Z}, so forward is {@code z} and starboard is {@code x}. */
    private static final Function<OceanOffset, Location> MOORED =
            offset -> new Location(null, offset.getRight(), 62, offset.getForward());

    private static Optional<Hazard> sweep(HazardField field, double despawnRadius) {
        return field.sweep(DIRECT, MOORED, HULL, despawnRadius);
    }

    @Test
    @DisplayName("a hazard in range is drawn where the projection puts it")
    void inRangeHazardsAreDrawn() {
        final HazardField field = new HazardField();
        final StubHazard hazard = StubHazard.at(40, 60, 5);
        field.add(hazard);

        assertTrue(sweep(field, 120).isEmpty());
        assertEquals(1, field.size());
        assertEquals(1, hazard.getRenders());

        assertNotNull(hazard.getLastRender());
        assertEquals(40, hazard.getLastRender().getX(), 1e-9);
        assertEquals(60, hazard.getLastRender().getZ(), 1e-9);
    }

    @Test
    @DisplayName("a hazard past the despawn radius leaves the field and takes its entities with it")
    void distantHazardsAreCulled() {
        final HazardField field = new HazardField();
        final StubHazard hazard = StubHazard.at(0, 130, 5);
        field.add(hazard);

        assertTrue(sweep(field, 120).isEmpty());
        assertEquals(0, field.size());
        assertEquals(1, hazard.getRemovals(), "culling has to clean up or entity-backed hazards are left behind");
        assertEquals(0, hazard.getRenders(), "and there is no point drawing something that has just gone");
    }

    @Test
    @DisplayName("a hazard freshly spawned at its spawn range is not culled on the tick it appears")
    void freshSpawnsSurviveTheirFirstTick() {
        final double spawnRange = HazardSpawnPolicy.DEFAULT.getAheadDistance();
        final HazardField field = new HazardField();
        final StubHazard hazard = StubHazard.at(0, spawnRange, 5);
        field.add(hazard);

        assertTrue(sweep(field, 120).isEmpty());
        assertEquals(1, field.size(), "a hazard deleted the tick it was created is a sea that is always empty");
        assertEquals(1, hazard.getRenders());
    }

    @Test
    @DisplayName("the configured despawn radius stands clear of every range hazards are spawned at")
    void despawnRadiusClearsTheSpawnRanges() throws NoSuchFieldException {
        final Config annotation = HazardConfig.class.getDeclaredField("despawnRadius").getAnnotation(Config.class);
        assertNotNull(annotation);

        final double despawnRadius = Double.parseDouble(annotation.defaultValue());
        assertTrue(despawnRadius > HazardSpawnPolicy.DEFAULT.getAheadDistance(),
                "hazards ahead spawn at " + HazardSpawnPolicy.DEFAULT.getAheadDistance()
                        + " and would be culled on arrival");
        assertTrue(despawnRadius > HazardSpawnPolicy.DEFAULT.getFlankDistance());
    }

    @Test
    @DisplayName("a hazard just outside the expanded hull is passed rather than hit")
    void nearMissIsNotAStrike() {
        final HazardField field = new HazardField();
        final StubHazard hazard = StubHazard.at(11.5, 0, 6);
        field.add(hazard);

        assertTrue(sweep(field, 120).isEmpty());
        assertEquals(1, field.size());
        assertEquals(0, hazard.getCollisions());
    }

    @Test
    @DisplayName("a hazard just inside the expanded hull is reported as a strike")
    void nearHitIsAStrike() {
        final HazardField field = new HazardField();
        final StubHazard hazard = StubHazard.at(10.5, 0, 6);
        field.add(hazard);

        assertEquals(hazard, sweep(field, 120).orElse(null));
    }

    @Test
    @DisplayName("a struck hazard leaves the field, so the same collision cannot fire twice")
    void aStrikeCannotDoubleFire() {
        final HazardField field = new HazardField();
        final StubHazard hazard = StubHazard.at(0, 0, 6);
        field.add(hazard);

        assertTrue(sweep(field, 120).isPresent());
        assertEquals(0, field.size());
        assertEquals(1, hazard.getRemovals());

        assertTrue(sweep(field, 120).isEmpty(), "the ship is still sitting on top of where it was");
    }

    @Test
    @DisplayName("clearing takes everything out of the world and hands the wheel back")
    void clearingEmptiesTheField() {
        final HazardField field = new HazardField();
        final StubHazard near = StubHazard.at(40, 40, 5);
        final StubHazard far = StubHazard.at(-40, 40, 5);
        field.add(near);
        field.add(far);
        field.getOverride().seize(1, 0, 10_000);

        field.clear();

        assertEquals(0, field.size());
        assertEquals(1, near.getRemovals());
        assertEquals(1, far.getRemovals());
        assertFalse(field.getOverride().isActive(0));
    }

    @Test
    @DisplayName("the snapshot of what is out there cannot be used to change it")
    void liveIsASnapshot() {
        final HazardField field = new HazardField();
        final StubHazard hazard = StubHazard.at(20, 20, 5);
        field.add(hazard);

        final List<Hazard> live = field.live();
        assertEquals(List.of(hazard), live);
        assertThrows(UnsupportedOperationException.class, live::clear);
    }
}
