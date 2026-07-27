package me.mykindos.betterpvp.core.item.impl.cannon.firing;

import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonState;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * What a cannon actually does when it goes off, and how it is loaded in the first place.
 * <p>
 * The prop handles the body, model, aiming, tags and persistence for every cannon and defers only the load and fire
 * semantics here, so a new kind of cannon needs no new prop subclass.
 *
 * @see ProjectileFiringMode
 * @see PassengerFiringMode
 */
public interface CannonFiringMode {

    /** Stable id, for logging and archetype definitions. */
    @NotNull String id();

    /**
     * Whether this mode is loaded by right-clicking the cannon with a cannonball item. Modes that load something other
     * than ammunition (a player, say) return {@code false} and take over {@link #onInteract} instead.
     */
    boolean consumesAmmo();

    /**
     * A non-sneaking right-click on the cannon that was not a reload and not an aim.
     *
     * @return {@code true} if the interaction was consumed (the caller cancels the Bukkit event)
     */
    default boolean onInteract(@NotNull CannonProp cannon, @NotNull Player player) {
        return false;
    }

    /**
     * A sneaking right-click: the operator asks the cannon to go off. Implementations either start the fuse or refuse.
     *
     * @return {@code true} if the request was accepted
     */
    boolean onTriggerRequested(@NotNull CannonProp cannon, @NotNull Player player);

    /**
     * The fuse has run out. Implementations launch whatever this cannon launches.
     * <p>
     * The returned state is what the cannon enters next: a projectile cannon returns {@link CannonState#COOLDOWN},
     * while a passenger cannon returns {@link CannonState#TARGETING} and later closes the cycle itself via
     * {@link me.mykindos.betterpvp.core.item.impl.cannon.model.CannonCycle#completeShot()}.
     *
     * @param operator   the operator if still online, else {@code null}
     * @param operatorId the operator's id, always present so credit survives a logout mid-fuse
     * @return the state the cannon should enter now
     */
    @NotNull CannonState onFuseComplete(@NotNull CannonProp cannon, @Nullable Player operator, @NotNull UUID operatorId);

    /**
     * The line of the cannon's floating instructions that tells a player what their next input does. The surrounding
     * tag (aim hint, fuse bar, cooldown timer) is rendered identically for every mode by the prop.
     */
    @NotNull Component actionLine(@NotNull CannonProp cannon);
}
