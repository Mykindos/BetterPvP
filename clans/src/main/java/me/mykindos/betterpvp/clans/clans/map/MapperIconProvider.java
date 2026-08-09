package me.mykindos.betterpvp.clans.clans.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.session.event.RegionsSavedEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.map.PointOfInterest;
import me.mykindos.betterpvp.core.map.events.MapPointOfInterestEvent;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.map.MapCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contributes map icons from Mapper datapoints, replacing the old hardcoded server-location icons. Any
 * {@link PointRegion} tagged {@code icon:<type>} becomes a map icon at that exact block, with an optional
 * {@code caption:<text>} — in any world, hot-reloaded when the map is saved. Placement and labelling are entirely
 * builder-authored.
 * <p>
 * Results are cached per world and rebuilt on {@link RegionsSavedEvent}, so the per-tick point-of-interest sweep just
 * reads the cache.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class MapperIconProvider implements Listener {

    private static final String ICON_TAG = "icon";
    private static final String CAPTION_TAG = "caption";
    private static final MapCursor.Type DEFAULT_TYPE = MapCursor.Type.RED_MARKER;

    private final Map<String, List<PointOfInterest>> iconsByWorld = new ConcurrentHashMap<>();

    @Inject
    public MapperIconProvider(Clans clans) {
        Bukkit.getPluginManager().registerEvents(this, clans);
    }

    @EventHandler
    public void onPointsOfInterest(MapPointOfInterestEvent event) {
        for (World world : Bukkit.getWorlds()) {
            event.getPointsOfInterest().addAll(iconsByWorld.computeIfAbsent(world.getName(), ignored -> build(world)));
        }
    }

    @EventHandler
    public void onRegionsSaved(RegionsSavedEvent event) {
        iconsByWorld.put(event.getWorld().getName(), build(event.getWorld()));
    }

    private List<PointOfInterest> build(World world) {
        final List<PointOfInterest> icons = new ArrayList<>();
        MapperHelper.readRegions(world).ifPresent(regions -> {
            for (Region region : regions) {
                if (!(region instanceof PointRegion point)) {
                    continue;
                }
                final RegionTags tags = RegionTags.of(region);
                if (!tags.has(ICON_TAG)) {
                    continue;
                }
                region.setWorld(world);
                final String caption = tags.getString(CAPTION_TAG, "").trim();
                icons.add(new PointOfInterest(point.getLocation(), caption.isEmpty() ? null : caption,
                        parseType(tags.getString(ICON_TAG, ""))));
            }
        });
        return icons;
    }

    private MapCursor.Type parseType(String raw) {
        try {
            return MapCursor.Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DEFAULT_TYPE;
        }
    }
}
