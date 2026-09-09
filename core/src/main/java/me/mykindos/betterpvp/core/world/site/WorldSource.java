package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Where an instance's world comes from.
 */
@Value
public class WorldSource {

    public enum Kind {
        /** An existing world on disk, used directly and never copied or deleted. */
        ADOPT,
        /** A template folder copied per instance and deleted when the instance is reaped. */
        CLONE,
        /** A folder under which each owner's world is kept, copied once and never deleted. */
        OWN
    }

    @NotNull Kind kind;

    /** A world name for {@link Kind#ADOPT}, otherwise a folder path. */
    @NotNull String value;

    public static @NotNull WorldSource adopt(@NotNull String worldName) {
        return new WorldSource(Kind.ADOPT, worldName);
    }

    public static @NotNull WorldSource clone(@NotNull String templateFolder) {
        return new WorldSource(Kind.CLONE, templateFolder);
    }

    public static @NotNull WorldSource own(@NotNull String folder) {
        return new WorldSource(Kind.OWN, folder);
    }
}
