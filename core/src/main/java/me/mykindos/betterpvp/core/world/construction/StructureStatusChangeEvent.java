package me.mykindos.betterpvp.core.world.construction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/**
 * A structure's {@link StructureStatus} changed: it started, finished, was claimed, paused, knocked out, repaired or
 * put away. Notices, visuals and permissions react to this rather than to the actions behind it.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StructureStatusChangeEvent extends CustomEvent {

    private final SiteKey site;
    private final PlacedStructure structure;
    private final StructureStatus from;
    private final StructureStatus to;

    public StructureStatusChangeEvent(@NotNull SiteKey site, @NotNull PlacedStructure structure,
                                      @NotNull StructureStatus from, @NotNull StructureStatus to) {
        this.site = site;
        this.structure = structure;
        this.from = from;
        this.to = to;
    }
}
