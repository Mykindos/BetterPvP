package me.mykindos.betterpvp.core.item.impl.cannon.firing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonFuseEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonDestination;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonState;
import me.mykindos.betterpvp.core.item.impl.cannon.ride.CannonRideService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The human cannonball: a click asks the clicker where they want to go, and picking somewhere boards them, lights the
 * fuse and launches them.
 * <p>
 * Shares the model, animations, aiming, tags, persistence and chunk lifecycle of every other cannon, differing only in
 * what it chambers and what happens when the fuse runs out.
 * <p>
 * On a {@link me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProperties#isPrivateOperation() private} cannon
 * the sequence belongs to the rider rather than to the emplacement: the cannon's own cycle is left alone, so the next
 * person can climb in behind them without waiting.
 */
@Singleton
public class PassengerFiringMode implements CannonFiringMode {

    private final Provider<CannonRideService> rides;

    @Inject
    private PassengerFiringMode(Provider<CannonRideService> rides) {
        this.rides = rides;
    }

    @Override
    public @NotNull String id() {
        return "passenger";
    }

    @Override
    public boolean consumesAmmo() {
        return false;
    }

    @Override
    public boolean onInteract(@NotNull CannonProp cannon, @NotNull Player player) {
        final boolean shared = !cannon.getProperties().isPrivateOperation();
        if (cannon.getCycle() == null) {
            return false;
        }
        // A shared cannon is one emplacement doing one thing at a time, so a cycle already running turns people away.
        // A private one is a queue of one-person rides that never touch the cannon's own state, so the only thing that
        // can turn someone away is already being aboard.
        if (shared && (cannon.getCycleState().isBusy() || !cannon.getCycle().isReady())) {
            return false;
        }
        if (rides.get().isRiding(player.getUniqueId())) {
            return false;
        }

        // Aim first, from the ground: the rider picks a destination, and only that choice puts them in the barrel and
        // lights the fuse. A shared emplacement is claimed for the whole choosing window so nobody climbs in behind a
        // rider who has not committed yet.
        if (shared) {
            cannon.getCycle().beginTargeting(player.getUniqueId());
        }
        rides.get().selectDestination(player, cannon,
                destination -> chamber(cannon, player, destination),
                () -> releaseCycle(cannon, player));
        return true;
    }

    /**
     * Puts a rider who has committed to a destination in the barrel and lights the fuse. Anything vetoing the fuse, or
     * a rider who can no longer board, hands a shared emplacement back rather than leaving it claimed.
     */
    private void chamber(@NotNull CannonProp cannon, @NotNull Player player, @Nullable CannonDestination destination) {
        final CannonFuseEvent event = new CannonFuseEvent(cannon, player);
        event.callEvent();
        if (event.isCancelled() || !rides.get().board(player, cannon)) {
            releaseCycle(cannon, player);
            return;
        }
        rides.get().beginFuse(player.getUniqueId(), destination);
    }

    /** Hands a shared emplacement back, if this player is the one still holding it. */
    private void releaseCycle(@NotNull CannonProp cannon, @NotNull Player player) {
        if (cannon.getCycle() != null && player.getUniqueId().equals(cannon.getCycle().getOperator())) {
            cannon.getCycle().abort();
        }
    }

    @Override
    public boolean onTriggerRequested(@NotNull CannonProp cannon, @NotNull Player player) {
        return onInteract(cannon, player);
    }

    @Override
    public @NotNull CannonState onFuseComplete(@NotNull CannonProp cannon, @Nullable Player operator,
                                               @NotNull UUID operatorId) {
        // The rider aimed before the fuse was ever lit, so there is nothing left to decide: they go now. A rider who
        // is no longer aboard - they logged out on the fuse - simply leaves the cannon to recover.
        rides.get().launch(operatorId);
        return CannonState.COOLDOWN;
    }

    @Override
    public @NotNull Component actionLine(@NotNull CannonProp cannon) {
        if (cannon.getCycleState().isBusy()) {
            return Component.empty();
        }
        return Component.text("Right-Click", NamedTextColor.WHITE, TextDecoration.BOLD)
                .append(Component.text(" to ", NamedTextColor.AQUA))
                .append(Component.text("Launch Yourself", NamedTextColor.GOLD, TextDecoration.BOLD));
    }
}
