package me.mykindos.betterpvp.core.world.construction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** A structure has been given an upgrade, either straight away or when the job fitting it was claimed. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StructureUpgradedEvent extends CustomEvent {

    private final SiteKey site;
    private final PlacedStructure structure;
    private final StructureUpgrade upgrade;

    public StructureUpgradedEvent(@NotNull SiteKey site, @NotNull PlacedStructure structure,
                                  @NotNull StructureUpgrade upgrade) {
        this.site = site;
        this.structure = structure;
        this.upgrade = upgrade;
    }
}
