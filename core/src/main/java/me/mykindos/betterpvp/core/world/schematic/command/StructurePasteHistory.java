package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What each builder has pasted this session, newest last, so {@code /structure undo} can take it back out. Held in
 * memory only: a builder's test pastes are not worth surviving a restart.
 */
@Singleton
public class StructurePasteHistory {

    private static final int LIMIT = 10;

    private final Map<UUID, Deque<Paste>> pastes = new HashMap<>();

    void record(@NotNull UUID builder, @NotNull World world, @NotNull List<Schematic.PlacedBlock> undo) {
        final Deque<Paste> history = pastes.computeIfAbsent(builder, key -> new ArrayDeque<>());
        history.addLast(new Paste(world.getName(), undo));
        if (history.size() > LIMIT) {
            history.removeFirst();
        }
    }

    @NotNull Optional<Paste> takeLatest(@NotNull UUID builder) {
        final Deque<Paste> history = pastes.get(builder);
        return history == null || history.isEmpty() ? Optional.empty() : Optional.of(history.removeLast());
    }

    @Value
    static class Paste {
        String world;
        List<Schematic.PlacedBlock> undo;
    }
}
