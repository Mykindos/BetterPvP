package me.mykindos.betterpvp.core.cutscene.camera;

import com.google.inject.Singleton;
import dev.brauw.mapper.Mapper;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.tag.PatternTag;
import dev.brauw.mapper.tag.RegionScope;
import dev.brauw.mapper.tag.TagRegistry;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a beat's camera id to a place in a world.
 * <p>
 * A camera angle is one {@code camera} data-point authored as a perspective marker - the facing is the shot, so a plain
 * point would lose half of it - carrying an {@code id:} tag that beats refer to. Ids are namespaced by convention
 * ({@code id:whats_new/harbour}) so two cutscenes may both have a "harbour" without colliding.
 * <p>
 * Indexed per world and cached, because the same cutscene is expected to run in every world cloned from a template and
 * each of those has its own copy of the markers. A world with no data-points (or with Mapper absent) reads as an empty
 * index rather than an error, which is the normal case for most worlds on the server.
 */
@Singleton
@BPvPListener
@CustomLog
public class CameraMarkers implements Reloadable, Listener {

    /** The Mapper data-point every camera angle is authored as. */
    public static final String CAMERA_POINT = "camera";

    private final Map<UUID, Map<String, PerspectiveRegion>> byWorld = new ConcurrentHashMap<>();

    private boolean tagsRegistered;

    /**
     * Teaches the map editor about the {@code camera} data-point once every plugin is up.
     * <p>
     * Done here rather than lazily on first lookup because the point of it is the editor: a builder placing markers
     * has usually not played a cutscene yet, and tags registered only once one runs would arrive after they were
     * needed.
     */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        registerTags();
    }

    private void registerTags() {
        if (tagsRegistered) {
            return;
        }
        tagsRegistered = true;

        try {
            final RegionScope camera = RegionScope.names(CAMERA_POINT);
            final TagRegistry tags = Mapper.get().getTagRegistry();
            tags.register(new PatternTag("id", "id:.+", "id:<text>",
                    "Names this camera angle, so a cutscene shot can point at it", true, camera));
            Mapper.get().getValidationRegistry().register(new CameraValidator());
        } catch (Throwable throwable) {
            // Editor convenience only - it must never stop cutscenes from playing. Throwable, not Exception: an older
            // Mapper surfaces here as NoClassDefFoundError/NoSuchMethodError.
            log.warn("Could not register camera tags - check the Mapper plugin version", throwable).submit();
        }
    }

    /**
     * @param cameraId the marker's {@code id:} tag, case-insensitive
     * @return where the camera sits and which way it looks, if that world declares the marker
     */
    public @NotNull Optional<Location> find(@NotNull World world, @NotNull String cameraId) {
        final PerspectiveRegion marker = index(world).get(cameraId.toLowerCase(java.util.Locale.ROOT));
        return marker == null ? Optional.empty() : Optional.of(marker.getLocation());
    }

    public @NotNull Optional<CameraPose> pose(@NotNull World world, @NotNull String cameraId) {
        return find(world, cameraId).map(CameraPose::of);
    }

    /** Every camera id this world declares - what {@code /cutscene markers} lists and the validator checks against. */
    public @NotNull Set<String> ids(@NotNull World world) {
        return Set.copyOf(index(world).keySet());
    }

    @Override
    public void reload() {
        byWorld.clear();
    }

    /** Drops one world's cached markers, for a builder who has just re-saved its data-points. */
    public void invalidate(@NotNull World world) {
        byWorld.remove(world.getUID());
    }

    private @NotNull Map<String, PerspectiveRegion> index(@NotNull World world) {
        return byWorld.computeIfAbsent(world.getUID(), key -> build(world));
    }

    private @NotNull Map<String, PerspectiveRegion> build(@NotNull World world) {
        try {
            return MapperHelper.readRegions(world)
                    .map(regions -> RegionIndex.of(world, regions).byId(CAMERA_POINT, PerspectiveRegion.class))
                    .orElseGet(Map::of);
        } catch (Exception exception) {
            // An unmapped world, or Mapper absent entirely. Both mean "no camera angles here", not a failure.
            return Map.of();
        }
    }
}
