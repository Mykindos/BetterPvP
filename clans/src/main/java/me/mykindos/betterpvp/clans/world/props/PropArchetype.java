package me.mykindos.betterpvp.clans.world.props;

import me.mykindos.betterpvp.core.scene.ScenePlacement;
import me.mykindos.betterpvp.core.scene.prop.SimpleProp;
import org.jetbrains.annotations.NotNull;

/**
 * Code behind a prop that needs more than tags can say.
 * <p>
 * Most props are entirely map data - a model, an ambient sound, some particles - and never come near this. An archetype
 * is for the rest: a prop that has to watch something, hold state, or drive an effect no tag describes. It is claimed by
 * name in {@link PropArchetypeRegistry} and asked for by a {@code type:} tag, so one archetype can back any number of
 * placements across any number of worlds.
 * <p>
 * Deliberately <em>not</em> how a prop becomes clickable: that is always the {@code interact:} tag and a
 * {@link me.mykindos.betterpvp.core.scene.interaction.SceneInteraction}, whether or not the prop has an archetype. One
 * mechanism for clicks, one for behaviour.
 */
@FunctionalInterface
public interface PropArchetype {

    /**
     * Attaches this archetype's behaviour to one placement. Called on <b>every</b> materialization - so a chunk cycle
     * re-runs it - which means it must build fresh behaviours rather than reuse ones it kept from last time.
     *
     * @param prop      the prop, with its entity present
     * @param placement which copy this is: its id, its tags, and the world it stands in
     */
    void decorate(@NotNull SimpleProp prop, @NotNull ScenePlacement placement);
}
