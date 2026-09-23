package me.mykindos.betterpvp.core.world.construction;

import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns Mapper cuboids named {@code build_zone} into zones structures may be built in. Any other tag on the cuboid
 * (for example {@code near_water}) is carried onto the zone, so a structure can ask for a particular kind of ground.
 * <p>
 * Content, so whichever module owns a kind of site binds it to the worlds it belongs in.
 */
public final class BuildZones implements WorldContent {

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final List<Zone> zones = new ArrayList<>();
        for (CuboidRegion region : regions.find(FitCheck.BUILD_ZONE, CuboidRegion.class)) {
            final Zone.ZoneBuilder builder = Zone.builder()
                    .key(Key.key("core", "build_zone_" + sanitise(world.getName()) + "_"
                            + region.getId().toString().toLowerCase(Locale.ROOT)))
                    .displayName(Component.text("Build zone"))
                    .bounds(RegionBounds.of(region))
                    .priority(0)
                    .rules(new ZoneRuleContainer())
                    .tag(FitCheck.BUILD_ZONE);
            RegionTags.of(region).markers().forEach(builder::tag);
            zones.add(builder.build());
        }
        return zones;
    }

    private static @NotNull String sanitise(@NotNull String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.\\-]", "_");
    }
}
