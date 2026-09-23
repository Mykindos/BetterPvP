package me.mykindos.betterpvp.core.world.settler;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Every settler one owner has at one site, kept in whatever record the owning module writes down. */
@Data
@NoArgsConstructor
public class Roster {

    private List<Settler> settlers = new ArrayList<>();

    public @NotNull Optional<Settler> find(@NotNull UUID id) {
        return settlers.stream().filter(settler -> settler.getId().equals(id)).findFirst();
    }

    public int size() {
        return settlers.size();
    }

    public @NotNull List<Settler> withProfession(@NotNull String profession) {
        return settlers.stream().filter(settler -> settler.hasProfession(profession)).toList();
    }

    public @NotNull List<Settler> assignedTo(@NotNull String workplace) {
        return settlers.stream().filter(settler -> workplace.equals(settler.getAssignment())).toList();
    }

    public @NotNull List<Settler> inState(@NotNull SettlerState state) {
        return settlers.stream().filter(settler -> settler.getState() == state).toList();
    }

    /** How many settlers of {@code profession} are assigned anywhere. */
    public int working(@NotNull String profession) {
        return (int) settlers.stream()
                .filter(settler -> settler.hasProfession(profession) && settler.getAssignment() != null)
                .count();
    }
}
