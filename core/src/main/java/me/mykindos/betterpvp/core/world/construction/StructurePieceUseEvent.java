package me.mykindos.betterpvp.core.world.construction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A player right-clicked a block of an upgrade's piece on a structure. Whatever the upgrade does when used sets
 * {@link #setHandled(boolean)}, which stops the click doing anything else, such as opening a container in the piece.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StructurePieceUseEvent extends CustomEvent {

    private final Player player;
    private final SiteKey site;
    private final PlacedStructure structure;
    private final StructureUpgrade upgrade;
    @Setter
    private boolean handled;

    public StructurePieceUseEvent(@NotNull Player player, @NotNull SiteKey site, @NotNull PlacedStructure structure,
                                  @NotNull StructureUpgrade upgrade) {
        this.player = player;
        this.site = site;
        this.structure = structure;
        this.upgrade = upgrade;
    }
}
