package me.mykindos.betterpvp.core.world.construction.blueprint;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;

/** Which structure type a blueprint places. */
@Getter
public class StructureBlueprintComponent extends AbstractItemComponent {

    private final String structure;

    public StructureBlueprintComponent(@NotNull String structure) {
        super("structure_blueprint");
        this.structure = structure;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new StructureBlueprintComponent(structure);
    }
}
