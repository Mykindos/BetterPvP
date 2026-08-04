package me.mykindos.betterpvp.clans.world.props;

import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.validation.ValidationIssue;
import me.mykindos.betterpvp.clans.world.content.DataPointValidator;
import me.mykindos.betterpvp.core.utilities.ModelTint;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.bukkit.Particle;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Checks prop markers while the builder is still standing next to them.
 * <p>
 * A prop fails quietly in every direction: the wrong region type is filtered out before its tags are read, an unknown
 * {@code type:} leaves it inert, and a misspelt particle simply never appears. All of them look like a prop that just
 * sits there, which is also what a correctly authored decorative prop looks like.
 *
 * @see WorldProps for what each tag means
 */
public class PropValidator extends DataPointValidator {

    private final PropArchetypeRegistry archetypes;

    public PropValidator(@NotNull PropArchetypeRegistry archetypes) {
        this.archetypes = archetypes;
    }

    @Override
    public String name() {
        return "props";
    }

    @Override
    public List<ValidationIssue> validate(World world, List<Region> regions) {
        final List<ValidationIssue> issues = new ArrayList<>();

        final List<Region> props = named(regions, WorldProps.PROP_POINT);
        if (props.isEmpty()) {
            return issues;
        }

        requireType(issues, props, PerspectiveRegion.class, "a perspective marker (facing matters)");
        collectIds(issues, props, "prop");

        for (Region prop : props) {
            final RegionTags tags = tags(prop);
            validateArchetype(issues, prop, tags);
            validateParticle(issues, prop, tags);
            validateColor(issues, prop, tags);
            validateInteraction(issues, prop, tags);
        }
        return issues;
    }

    private void validateArchetype(List<ValidationIssue> issues, Region prop, RegionTags tags) {
        final String type = tags.getString("type", "").trim();
        if (!type.isEmpty() && archetypes.get(type) == null) {
            issues.add(ValidationIssue.error(prop,
                    "Type '" + type + "' is not a registered prop archetype. Available: " + String.join(", ", archetypes.keys())));
        }
    }

    private void validateParticle(List<ValidationIssue> issues, Region prop, RegionTags tags) {
        final String particle = tags.getString("particle", "").trim();
        if (particle.isEmpty()) {
            return;
        }

        try {
            Particle.valueOf(particle.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            issues.add(ValidationIssue.error(prop, "'" + particle + "' is not a particle"));
        }
    }

    private void validateColor(List<ValidationIssue> issues, Region prop, RegionTags tags) {
        final String color = tags.getString("color", "").trim();
        if (!color.isEmpty() && ModelTint.parse(color).isEmpty()) {
            issues.add(ValidationIssue.error(prop, "'" + color + "' is not a colour - write hex (#ff8800) or a "
                    + "colour name (red), optionally per bone (flame=ffdd66), separated by commas"));
        }
    }

    /**
     * A named action that nothing claims leaves a prop that visibly invites a click and then does nothing - the worst
     * failure of the set, because it reads as a bug to every player who tries it.
     */
    private void validateInteraction(List<ValidationIssue> issues, Region prop, RegionTags tags) {
        final String interaction = tags.getString("interact", "").trim();
        final boolean hasAnimation = !tags.getString("interact_animation", "").isBlank();
        if (interaction.isEmpty() && hasAnimation) {
            issues.add(ValidationIssue.warning(prop,
                    "Has an 'interact_animation' but no 'interact', so it is never clicked and the animation never plays"));
        }
    }
}
