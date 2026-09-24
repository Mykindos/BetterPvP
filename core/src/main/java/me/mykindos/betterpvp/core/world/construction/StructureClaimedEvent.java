package me.mykindos.betterpvp.core.world.construction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** A finished job has been claimed by {@link #getPlayer()}, and whatever it did now applies to its structure. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StructureClaimedEvent extends CustomEvent {

    private final SiteKey site;
    private final World world;
    private final PlacedStructure structure;
    private final Job job;
    private final Player player;

    public StructureClaimedEvent(@NotNull SiteKey site, @NotNull World world, @NotNull PlacedStructure structure,
                                 @NotNull Job job, @NotNull Player player) {
        this.site = site;
        this.world = world;
        this.structure = structure;
        this.job = job;
        this.player = player;
    }
}
