package me.mykindos.betterpvp.clans.world.spawn;

import dev.brauw.mapper.region.PathRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.validation.RegionValidator;
import dev.brauw.mapper.validation.ValidationIssue;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Checks the spawn resident data-points while the builder is still standing next to them.
 * <p>
 * Every rule here corresponds to a way {@link SpawnResidents} can fail to build a villager. Those
 * failures are all quiet: a resident authored as the wrong region type is filtered out before the
 * loader ever sees it, a display whose {@code resident:} names nobody is skipped without a word, and
 * a dangling {@code route:} only produces a warning in a startup log the builder is not reading. All
 * of them look identical in-world - the NPC is simply not there - and all of them surface hours
 * after the mistake, in a server log, rather than at the moment of authoring.
 *
 * @see SpawnResidents for what each data-point and tag means
 */
public class SpawnResidentValidator implements RegionValidator {

    @Override
    public String name() {
        return "spawn-residents";
    }

    @Override
    public List<ValidationIssue> validate(World world, List<Region> regions) {
        final List<ValidationIssue> issues = new ArrayList<>();

        final List<Region> residents = named(regions, SpawnResidents.RESIDENT_POINT);
        final List<Region> routes = named(regions, SpawnResidents.ROUTE_POINT);
        final List<Region> displays = named(regions, SpawnResidents.DISPLAY_POINT);
        if (residents.isEmpty() && routes.isEmpty() && displays.isEmpty()) {
            return issues;
        }

        // The loader looks each data-point up by region name *and* type, so a marker authored with the
        // wrong wand is dropped before any tag on it is ever read.
        requireType(issues, residents, PerspectiveRegion.class, "a perspective marker (facing matters)");
        requireType(issues, routes, PathRegion.class, "a path");
        requireType(issues, displays, PointRegion.class, "a point");

        final Map<String, Region> residentIds = collectIds(issues, residents, "resident");
        final Map<String, Region> routeIds = collectIds(issues, routes, "route");

        validateRoutes(issues, routes);
        validateResidents(issues, residents, routeIds);
        validateDisplays(issues, displays, residentIds);
        return issues;
    }

    /**
     * Indexes data-points by their {@code id:} tag, reporting any two that claim the same one.
     * <p>
     * A repeated id is an error rather than a warning because the loader resolves references with
     * {@code findFirst}: the second holder is not merely ambiguous, it is unreachable, and which of
     * the two wins depends on file order.
     */
    private Map<String, Region> collectIds(List<ValidationIssue> issues, List<Region> regions, String what) {
        final Map<String, Region> byId = new HashMap<>();
        for (Region region : regions) {
            final String id = tags(region).getString("id", "").trim();
            if (id.isEmpty()) {
                continue;
            }

            final Region clash = byId.putIfAbsent(id.toLowerCase(Locale.ROOT), region);
            if (clash != null) {
                issues.add(ValidationIssue.error(region,
                        "Another " + what + " already uses id '" + id + "' - only one of them can be referenced"));
            }
        }
        return byId;
    }

    private void validateRoutes(List<ValidationIssue> issues, List<Region> routes) {
        for (Region route : routes) {
            if (tags(route).getString("id", "").isBlank()) {
                issues.add(ValidationIssue.warning(route,
                        "Route has no 'id' tag, so no resident can name it"));
            }

            // Two waypoints is the minimum the patrol behaviour accepts; below that the resident it
            // belongs to silently stands still instead.
            if (route instanceof PathRegion path && path.size() < 2) {
                issues.add(ValidationIssue.warning(route,
                        "Route has " + path.size() + " waypoint(s) - at least 2 are needed to walk it"));
            }
        }
    }

    private void validateResidents(List<ValidationIssue> issues, List<Region> residents, Map<String, Region> routeIds) {
        for (Region resident : residents) {
            final String routeId = tags(resident).getString("route", "").trim();
            if (!routeId.isEmpty() && !routeIds.containsKey(routeId.toLowerCase(Locale.ROOT))) {
                issues.add(ValidationIssue.error(resident,
                        "Walks route '" + routeId + "', which no npc_route declares"));
            }
        }
    }

    private void validateDisplays(List<ValidationIssue> issues, List<Region> displays, Map<String, Region> residentIds) {
        for (Region display : displays) {
            final RegionTags tags = tags(display);

            final String residentId = tags.getString("resident", "").trim();
            if (residentId.isEmpty()) {
                issues.add(ValidationIssue.error(display,
                        "Display has no 'resident' tag, so nobody shows it"));
            } else if (!residentIds.containsKey(residentId.toLowerCase(Locale.ROOT))) {
                issues.add(ValidationIssue.error(display,
                        "Belongs to resident '" + residentId + "', which no npc_resident declares"));
            }

            final String item = tags.getString("item", "").trim();
            if (item.isEmpty()) {
                issues.add(ValidationIssue.error(display, "Display has no 'item' tag"));
            } else if (Material.matchMaterial(item.toUpperCase(Locale.ROOT)) == null) {
                issues.add(ValidationIssue.error(display, "'" + item + "' is not a material"));
            }
        }
    }

    /**
     * Reports data-points authored with the wrong region type. Reported once per offender rather than
     * once per name, since a builder who got one wrong usually got exactly one wrong.
     */
    private void requireType(List<ValidationIssue> issues, List<Region> regions,
                             Class<? extends Region> required, String description) {
        for (Region region : regions) {
            if (!required.isInstance(region)) {
                issues.add(ValidationIssue.error(region,
                        "Must be " + description + ", but was authored as a "
                                + region.getType().name().toLowerCase(Locale.ROOT)));
            }
        }
    }

    private List<Region> named(List<Region> regions, String name) {
        return regions.stream().filter(region -> name.equalsIgnoreCase(region.getName())).toList();
    }

    private RegionTags tags(@NotNull Region region) {
        final Set<String> applied = region.getOptions().getTags();
        return new RegionTags(applied == null ? new HashSet<>() : applied);
    }
}
