package me.mykindos.betterpvp.core.scene;

import lombok.Value;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * How and where one scene object was authored: the marker's identity, its tags, and the spot it sits on.
 * <p>
 * Passed to whatever the marker points at - a named {@code interact:} action, a prop archetype - so that code can tell
 * one placement of itself from another. This matters most on instanced worlds: two islands cloned from the same
 * template contain byte-identical markers, and without the placement a handler has no way to know which copy it is
 * standing on. The {@link #getWorld() world} is the instance.
 * <p>
 * The {@link #getObject() object} outlives its backing entity, so holding one of these across a chunk cycle is safe.
 */
@Value
public class ScenePlacement {

    /** The scene object this marker produced. */
    SceneObject object;

    /** The marker's {@code id:} tag, or empty if it does not have one. */
    String id;

    /** Everything else authored on the marker, so a handler can read its own configuration. */
    RegionTags tags;

    /** Where the marker sits, in the world this placement belongs to. */
    Location location;

    public @NotNull World getWorld() {
        return location.getWorld();
    }
}
