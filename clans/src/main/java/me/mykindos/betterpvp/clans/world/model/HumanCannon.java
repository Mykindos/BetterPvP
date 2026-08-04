package me.mykindos.betterpvp.clans.world.model;

import dev.brauw.mapper.region.PointRegion;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonArchetypeRegistry;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonDestination;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProperties;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A launcher cannon that fires whoever climbs into it, placed at every {@code human_cannon} data-point.
 * <p>
 * Map-declared rather than player-placed, so it is rebuilt from the data-point on each load and never persisted -
 * moving or deleting the point is enough to move or remove the cannon.
 * <p>
 * Where it can fire people is map data too: each entry in {@link #DESTINATIONS} is the label shown in the rider's
 * menu paired with the data-point that marks the landing site. A destination whose point is missing from the map is
 * simply not offered.
 */
public class HumanCannon implements WorldContent {

    private static final String DATA_POINT = "human_cannon";

    /** Destination label to the data-point marking where it lands. Placeholder set until the map is built out. */
    private static final Map<String, String> DESTINATIONS = Map.of(
            "Dock", "cannon_dock",
            "Plaza", "cannon_plaza");

    private final CannonService cannonService;

    public HumanCannon(CannonService cannonService) {
        this.cannonService = cannonService;
    }

    @Override
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull RegionIndex regions) {
        final List<SceneSpawn> sceneObjects = new ArrayList<>();
        final List<CannonDestination> destinations = destinations(regions);

        for (PointRegion point : regions.find(DATA_POINT, PointRegion.class)) {
            final Location location = point.getLocation();
            final CannonProp cannon = cannonService.create(CannonArchetypeRegistry.LAUNCHER, properties());
            cannon.setDestinations(destinations);
            sceneObjects.add(new SceneSpawn(cannon, location, cannonService.bodyFactory(cannon)));
        }

        return sceneObjects;
    }

    /** Resolves every {@link #DESTINATIONS} entry whose data-point exists in this world. */
    private static @NotNull List<CannonDestination> destinations(@NotNull RegionIndex regions) {
        final List<CannonDestination> destinations = new ArrayList<>();
        DESTINATIONS.forEach((name, dataPoint) -> regions.findOne(dataPoint, PointRegion.class)
                .ifPresent(point -> destinations.add(CannonDestination.of(name, point.getLocation()))));
        return destinations;
    }

    /**
     * A fixed, indestructible emplacement: players ride it, they do not fight over it.
     * <p>
     * It runs privately, so a spawn crowded with people launching themselves stays quiet for everyone not currently
     * in the barrel, and it drops the progress line - a rider needs the prompt to click, not a readout of the fuse.
     */
    private static @NotNull CannonProperties properties() {
        return CannonProperties.builder()
                .invincible(true)
                .removable(false)
                .movable(false)
                .showHealthBar(false)
                .breaksBlocks(false)
                .allowRotation(false)
                .damageMultiplier(0)
                .power(500)
                .size(1.5)
                .instructionsViewDistance(20)
                .showProgressLine(false)
                .privateOperation(true)
                .build();
    }
}
