package me.mykindos.betterpvp.core.world.construction.blueprint;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Which structure type a blueprint places. A blueprint bound to a placed structure moves that structure instead of
 * building a new one.
 */
@Getter
public class StructureBlueprintComponent extends AbstractItemComponent {

    private final String structure;
    private final @Nullable UUID moving;

    public StructureBlueprintComponent(@NotNull String structure) {
        this(structure, null);
    }

    public StructureBlueprintComponent(@NotNull String structure, @Nullable UUID moving) {
        super("structure_blueprint");
        this.structure = structure;
        this.moving = moving;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new StructureBlueprintComponent(structure, moving);
    }
}
