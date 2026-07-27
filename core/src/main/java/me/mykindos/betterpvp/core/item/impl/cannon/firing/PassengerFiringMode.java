package me.mykindos.betterpvp.core.item.impl.cannon.firing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonFuseEvent;
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
 * The human cannonball: a single click boards the clicker, and the whole sequence runs from there - fuse, aim, launch.
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

        final CannonFuseEvent event = new CannonFuseEvent(cannon, player);
        event.callEvent();
        if (event.isCancelled()) {
            return false;
        }

        if (!rides.get().board(player, cannon)) {
            return false;
        }

        // One gesture does both: there is no second click to light the fuse.
        if (shared) {
            cannon.getCycle().beginBoarding(player.getUniqueId());
            cannon.getCycle().beginFuse(player.getUniqueId());
        } else {
            rides.get().beginFuse(player.getUniqueId());
        }
        return true;
    }

    @Override
    public boolean onTriggerRequested(@NotNull CannonProp cannon, @NotNull Player player) {
        return onInteract(cannon, player);
    }

    @Override
    public @NotNull CannonState onFuseComplete(@NotNull CannonProp cannon, @Nullable Player operator,
                                               @NotNull UUID operatorId) {
        // The fuse is spent, but the shot is not over: the rider now picks where to land, and the ride closes the
        // cycle itself once they are away. A rider who is no longer aboard - they logged out on the fuse - leaves
        // nothing to wait for, so the cannon recovers instead of holding a shot that will never be taken.
        return rides.get().beginTargeting(operatorId) ? CannonState.TARGETING : CannonState.COOLDOWN;
    }

    @Override
    public @NotNull Component actionLine(@NotNull CannonProp cannon) {
        if (cannon.getCycleState() == CannonState.TARGETING) {
            return Component.text("Look", NamedTextColor.WHITE, TextDecoration.BOLD)
                    .append(Component.text(" to ", NamedTextColor.AQUA))
                    .append(Component.text("Aim", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        }
        if (cannon.getCycleState().isBusy()) {
            return Component.empty();
        }
        return Component.text("Right-Click", NamedTextColor.WHITE, TextDecoration.BOLD)
                .append(Component.text(" to ", NamedTextColor.AQUA))
                .append(Component.text("Launch Yourself", NamedTextColor.GOLD, TextDecoration.BOLD));
    }
}
