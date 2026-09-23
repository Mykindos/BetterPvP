package me.mykindos.betterpvp.core.world.construction;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A kind of building that can be raised. Each type is code, supplied by whatever module owns the content, while its
 * numbers come from that module's config.
 */
public interface StructureType {

    @NotNull String getId();

    @NotNull Component getDisplayName();

    /** Which tier it belongs to, for whatever gates tiers. */
    int getTier();

    /** Ids of the structure types that must already stand before this one can be built. */
    @NotNull Set<String> getRequiredStructures();

    /** A tag the build zone it goes in must carry, or null if any build zone will do. */
    @Nullable String getRequiredZoneTag();

    /** The version chain, first to last. Version 0 is what building it produces. Never empty. */
    @NotNull List<StructureVersion> getVersions();

    @NotNull StructureFlags getFlags();

    default @NotNull StructureVersion version(int index) {
        return getVersions().get(Math.clamp(index, 0, getVersions().size() - 1));
    }

    default boolean hasVersion(int index) {
        return index >= 0 && index < getVersions().size();
    }
}
