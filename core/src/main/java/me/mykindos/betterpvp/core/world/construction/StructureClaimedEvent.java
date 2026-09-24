package me.mykindos.betterpvp.core.world.construction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/** A finished job has been claimed, and whatever it did now applies to its structure. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StructureClaimedEvent extends CustomEvent {

    private final SiteKey site;
    private final World world;
    private final PlacedStructure structure;
    private final Job job;

    public StructureClaimedEvent(@NotNull SiteKey site, @NotNull World world, @NotNull PlacedStructure structure,
                                 @NotNull Job job) {
        this.site = site;
        this.world = world;
        this.structure = structure;
        this.job = job;
    }
}
