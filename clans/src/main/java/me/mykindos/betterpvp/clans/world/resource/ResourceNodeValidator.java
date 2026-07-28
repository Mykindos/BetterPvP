package me.mykindos.betterpvp.clans.world.resource;

import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.validation.RegionValidator;
import dev.brauw.mapper.validation.ValidationIssue;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Checks that resource-node regions are the shape their archetype needs.
 * <p>
 * A node definition binds a region <em>name</em> to an archetype, and the archetype decides what
 * kind of region it can read - an ore field is a cuboid, a tree is a perspective marker. The loader
 * matches on both, so a region carrying the right name but authored with the wrong wand is filtered
 * out before anything looks at it: no node, no warning, and a builder staring at a marker that
 * appears correct.
 * <p>
 * The expected types come from the loaded definitions, so this validates against whatever nodes the
 * server actually has rather than a list duplicated here.
 */
public class ResourceNodeValidator implements RegionValidator {

    private final Map<String, Region.RegionType> expectedTypes;

    /**
     * @param expectedTypes region name (lower-cased) to the region type its archetype requires
     */
    public ResourceNodeValidator(@NotNull Map<String, Region.RegionType> expectedTypes) {
        this.expectedTypes = Map.copyOf(expectedTypes);
    }

    @Override
    public String name() {
        return "resource-nodes";
    }

    @Override
    public List<ValidationIssue> validate(World world, List<Region> regions) {
        final List<ValidationIssue> issues = new ArrayList<>();
        for (Region region : regions) {
            final Region.RegionType expected = expectedTypes.get(region.getName().toLowerCase(Locale.ROOT));
            if (expected != null && region.getType() != expected) {
                issues.add(ValidationIssue.error(region,
                        "Resource node '" + region.getName() + "' must be a "
                                + expected.name().toLowerCase(Locale.ROOT) + " region, but was authored as a "
                                + region.getType().name().toLowerCase(Locale.ROOT)));
            }
        }
        return issues;
    }
}
