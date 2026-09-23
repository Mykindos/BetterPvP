package me.mykindos.betterpvp.core.world.construction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** A structure left its holding, demolished or cancelled before it was first finished. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StructureRemovedEvent extends CustomEvent {

    private final SiteKey site;
    private final PlacedStructure structure;
    private final boolean demolished;

    public StructureRemovedEvent(@NotNull SiteKey site, @NotNull PlacedStructure structure, boolean demolished) {
        this.site = site;
        this.structure = structure;
        this.demolished = demolished;
    }
}
