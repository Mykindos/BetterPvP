package me.mykindos.betterpvp.core.world.settler.presence;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** The body of one settler. Its look and routine are put on it each time it materializes. */
@Getter
public class SettlerNPC extends ModeledNPC {

    private final UUID settlerId;
    @Setter(AccessLevel.PACKAGE)
    private @Nullable SettlerRoutine routine;

    public SettlerNPC(@NotNull SceneObjectFactory factory, @NotNull UUID settlerId) {
        super(factory);
        this.settlerId = settlerId;
    }

    SettlerNPC(@NotNull SceneObjectFactory factory, @NotNull UUID settlerId,
               @NotNull Supplier<Optional<Location>> post, @NotNull LongSupplier clock) {
        super(factory);
        throw new UnsupportedOperationException("Not implemented yet, see #2368");
    }

    /** Sends it to its new workplace, or off to wander, straight away. */
    public void replan() {
        if (routine != null && isMaterialized()) {
            routine.replan();
        }
    }

    /** Stops to face {@code player}, who is talking to it. */
    public void talkTo(@NotNull Player player) {
        if (routine != null && isMaterialized()) {
            routine.talkTo(player);
        }
    }

    /** Sends it to stand near {@code spot} for a moment before it goes back to its day. */
    public void gather(@NotNull Location spot) {
        if (routine != null && isMaterialized()) {
            routine.gather(spot);
        }
    }
}
