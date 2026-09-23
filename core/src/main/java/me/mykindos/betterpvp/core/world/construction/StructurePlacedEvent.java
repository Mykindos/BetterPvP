package me.mykindos.betterpvp.core.world.construction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** A structure joined a holding, by being built or granted. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StructurePlacedEvent extends CustomEvent {

    private final SiteKey site;
    private final PlacedStructure structure;

    public StructurePlacedEvent(@NotNull SiteKey site, @NotNull PlacedStructure structure) {
        this.site = site;
        this.structure = structure;
    }
}
