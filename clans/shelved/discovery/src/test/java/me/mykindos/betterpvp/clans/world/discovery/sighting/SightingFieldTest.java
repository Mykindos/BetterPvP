package me.mykindos.betterpvp.clans.world.discovery.sighting;

import me.mykindos.betterpvp.clans.world.discovery.OceanOffset;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import org.bukkit.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SightingFieldTest {

    /** The shipped ordering, so the two decisions are tested against the distances the sea actually runs on. */
    private static final SightingConfig CONFIG = SightingConfig.builder()
            .sightingsPerSecond(0.03)
            .arrivalDistance(100.0)
            .rampStartDistance(200.0)
            .pinDistance(100.0)
            .spawnMinDistance(600.0)
            .spawnMaxDistance(1200.0)
            .despawnDistance(1500.0)
            .build();

    /** The plane's coordinates read straight back as a ship-local offset, so a test can place islands by hand. */
    private static final Function<OceanPoint, OceanOffset> DIRECT =
            point -> new OceanOffset(point.getX(), point.getZ());

    /** A ship moored on the origin facing {@code +Z}, so forward is {@code z} and starboard is {@code x}. */
    private static final Function<OceanOffset, Location> MOORED =
            offset -> new Location(null, offset.getRight(), 62, offset.getForward());

    private static Optional<Sighting> sweep(SightingField field) {
        return field.sweep(DIRECT, MOORED, CONFIG, true);
    }

    @Test
    @DisplayName("an island past the despawn distance leaves the field and takes its displays with it")
    void distantSightingsAreCulled() {
        final SightingField field = new SightingField();
        final StubSighting sighting = StubSighting.at(0, 1600);
        field.add(sighting);

        assertTrue(sweep(field).isEmpty());
        assertEquals(0, field.size());
        assertEquals(1, sighting.getRemovals(), "culling has to clean up or the displays are left in a dead world");
        assertEquals(0, sighting.getRenders(), "and there is no point drawing something that has just gone");
    }

    @Test
    @DisplayName("an island inside the arrival distance is the one the crew has reached")
    void arrivalIsReported() {
        final SightingField field = new SightingField();
        final StubSighting sighting = StubSighting.at(0, 80);
        field.add(sighting);

        assertSame(sighting, sweep(field).orElse(null));
        assertEquals(1, sighting.getRemovals());
    }

    @Test
    @DisplayName("a reached island leaves the field before it is handed back, so landfall cannot fire twice")
    void arrivalCannotDoubleFire() {
        final SightingField field = new SightingField();
        field.add(StubSighting.at(0, 80));

        assertTrue(sweep(field).isPresent());
        assertEquals(0, field.size(), "the island is still under the bow and would be reached again next tick");

        assertTrue(sweep(field).isEmpty(), "a second landing would teleport a landed crew off their island");
    }

    @Test
    @DisplayName("an island between arrival and despawn is only drawn, at its true distance")
    void middlingSightingsAreDrawn() {
        final SightingField field = new SightingField();
        final StubSighting sighting = StubSighting.at(0, 500);
        field.add(sighting);

        assertTrue(sweep(field).isEmpty());
        assertEquals(1, field.size());
        assertEquals(0, sighting.getRemovals());
        assertEquals(1, sighting.getRenders());
        assertEquals(500, sighting.getLastDistance(), 1e-9, "the crew is told how far it really is, not where it is pinned");
    }

    @Test
    @DisplayName("only the first island reached is landed at; the rest stay on the sea")
    void oneLandingAtATime() {
        final SightingField field = new SightingField();
        final StubSighting nearer = StubSighting.at(0, 40);
        final StubSighting alsoClose = StubSighting.at(0, 60);
        field.add(nearer);
        field.add(alsoClose);

        assertSame(nearer, sweep(field).orElse(null));
        assertEquals(1, field.size(), "a crew can only go ashore in one place");
        assertEquals(0, alsoClose.getRemovals());
    }
}
