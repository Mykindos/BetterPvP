package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        /** A folder under which each owner's world is kept, made once and never deleted. */
        OWN
    }

    @NotNull Kind kind;

    /** A world name for {@link Kind#ADOPT}, otherwise a folder path. */
    @NotNull String value;

    /**
     * What an owner's world is first made from, or null to generate one. Only {@link Kind#OWN} uses it, and only the
     * first time that owner is given a world, so editing it afterwards changes nothing that already exists.
     */
    @Nullable String template;

    public static @NotNull WorldSource adopt(@NotNull String worldName) {
        return new WorldSource(Kind.ADOPT, worldName, null);
    }

    public static @NotNull WorldSource clone(@NotNull String templateFolder) {
        return new WorldSource(Kind.CLONE, templateFolder, null);
    }

    public static @NotNull WorldSource own(@NotNull String folder) {
        return new WorldSource(Kind.OWN, folder, null);
    }

    public static @NotNull WorldSource own(@NotNull String folder, @Nullable String template) {
        return new WorldSource(Kind.OWN, folder, template);
    }
}
