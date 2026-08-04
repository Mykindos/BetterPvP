package me.mykindos.betterpvp.clans.world.discovery.sighting;

import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import me.mykindos.betterpvp.clans.world.island.IslandOffer;
import me.mykindos.betterpvp.clans.world.island.IslandTemplate;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A sighting that records what was asked of it, so the field's decisions can be read back without a world. */
class StubSighting extends Sighting {

    private static final IslandTemplate TEMPLATE =
            new IslandTemplate("stub", Component.text("Stub"), "islands/stub", Material.GRASS_BLOCK);

    private int renders;
    private int removals;
    private Location lastRender;
    private double lastDistance;

    private StubSighting(@NotNull OceanPoint point, @NotNull IslandOffer offer) {
        super(point, offer);
    }

    static @NotNull StubSighting at(double x, double z) {
        final IslandOffer offer = mock(IslandOffer.class);
        when(offer.getTemplate()).thenReturn(TEMPLATE);
        return new StubSighting(new OceanPoint(x, z), offer);
    }

    int getRenders() {
        return renders;
    }

    int getRemovals() {
        return removals;
    }

    Location getLastRender() {
        return lastRender;
    }

    double getLastDistance() {
        return lastDistance;
    }

    @Override
    public void render(@NotNull Location at, double trueDistance, double arrivalDistance, double rampStartDistance,
                       boolean refreshLabel) {
        renders++;
        lastRender = at;
        lastDistance = trueDistance;
    }

    @Override
    public void remove() {
        removals++;
    }
}
