package me.mykindos.betterpvp.core.cutscene.camera;

import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.validation.RegionValidator;
import dev.brauw.mapper.validation.ValidationIssue;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Checks camera markers while the builder is still standing next to them.
 * <p>
 * Every mistake a camera marker can make is silent. Drawn with the wrong wand it is filtered out by type before its
 * tags are read; without an {@code id:} nothing can ever refer to it; sharing an id with another marker makes one of
 * the two permanently unreachable. All three look identical from in-world - the shot simply is not there - and all
 * three surface later as a cutscene refusing to start in a world that looks correctly marked up.
 *
 * @see CameraMarkers for what the data-point means
 */
public class CameraValidator implements RegionValidator {

    @Override
    public String name() {
        return "cutscene cameras";
    }

    @Override
    public List<ValidationIssue> validate(World world, List<Region> regions) {
        final List<ValidationIssue> issues = new ArrayList<>();
        final List<Region> cameras = regions.stream()
                .filter(region -> CameraMarkers.CAMERA_POINT.equalsIgnoreCase(region.getName()))
                .toList();
        if (cameras.isEmpty()) {
            return issues;
        }

        final Map<String, Region> byId = new HashMap<>();
        for (Region camera : cameras) {
            if (!(camera instanceof PerspectiveRegion)) {
                issues.add(ValidationIssue.error(camera,
                        "Must be a perspective marker - where a camera looks is the shot - but was authored as a "
                                + camera.getType().name().toLowerCase(Locale.ROOT)));
                continue;
            }

            final String id = RegionTags.of(camera).getString("id", "").trim();
            if (id.isEmpty()) {
                issues.add(ValidationIssue.error(camera,
                        "Needs an 'id:' tag - a shot names its camera by id, so an untagged one can never be used"));
                continue;
            }

            // An error rather than a warning: lookups resolve to the first holder, so the second is unreachable and
            // which of the two wins depends on nothing more than file order.
            final Region existing = byId.putIfAbsent(id.toLowerCase(Locale.ROOT), camera);
            if (existing != null) {
                issues.add(ValidationIssue.error(camera, "Another camera already uses the id '" + id + "'"));
            }
        }
        return issues;
    }
}
