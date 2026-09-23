package me.mykindos.betterpvp.core.world.construction;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every structure one owner has in one place. It is data only, kept in whatever record the owning module writes down,
 * so it survives the world it stands in being closed or rebuilt.
 */
@Data
@NoArgsConstructor
public class Holding {

    private List<PlacedStructure> structures = new ArrayList<>();

    public @NotNull Optional<PlacedStructure> find(@NotNull UUID id) {
        return structures.stream().filter(structure -> structure.getId().equals(id)).findFirst();
    }

    public @NotNull List<PlacedStructure> ofType(@NotNull String type) {
        return structures.stream().filter(structure -> structure.getType().equals(type)).toList();
    }

    /** Whether a structure of {@code type} stands finished, which is what counts as having it for requirements. */
    public boolean hasBuilt(@NotNull String type) {
        return ofType(type).stream().anyMatch(structure -> structure.getCondition() != StructureCondition.UNDER_CONSTRUCTION
                && structure.getCondition() != StructureCondition.NOT_PLACED);
    }
}
