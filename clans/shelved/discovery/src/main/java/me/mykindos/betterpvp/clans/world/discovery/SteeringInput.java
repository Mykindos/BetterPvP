package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Who is holding a ship's two steering controls, and what the pair adds up to.
 * <p>
 * There is no "release" packet for a held right-click: the vanilla client simply re-sends the interact roughly every
 * 250 ms while the button is down and stops when it is not. So a control is not held or unheld, it is <em>recently
 * pressed</em> — {@link #GRACE_MILLIS} is that re-send plus enough headroom that one late tick does not read as the
 * crewman letting go.
 * <p>
 * A control takes one pair of hands. The first claimant keeps it until their grace lapses, and everyone else is refused
 * rather than queued, because two people alternating clicks on the same control would otherwise read as one very fast
 * one.
 * <p>
 * Nothing here touches the world, so the whole question of what the crew is asking the ship to do is answerable without
 * a server.
 */
public class SteeringInput {

    /** How long one interact keeps a control live. */
    public static final long GRACE_MILLIS = 350L;

    /** Why a press did or did not take. */
    public enum Response {
        ACCEPTED,
        /** Not on this expedition's roster — passengers do not steer. */
        NOT_CREW,
        /** Somebody else has this control and has not let go of it. */
        HELD_BY_ANOTHER
    }

    @Value
    private static class Hold {
        UUID holder;
        long at;
    }

    private final Predicate<UUID> crew;
    private final Map<SteerSide, Hold> holds = new EnumMap<>(SteerSide.class);

    public SteeringInput(@NotNull Predicate<UUID> crew) {
        this.crew = crew;
    }

    /** Records one interact on {@code side}. */
    public @NotNull Response press(@NotNull SteerSide side, @NotNull UUID player, long now) {
        if (!crew.test(player)) {
            return Response.NOT_CREW;
        }

        final Hold hold = holds.get(side);
        if (hold != null && !hold.getHolder().equals(player) && isLive(hold, now)) {
            return Response.HELD_BY_ANOTHER;
        }

        holds.put(side, new Hold(player, now));
        return Response.ACCEPTED;
    }

    public boolean isActive(@NotNull SteerSide side, long now) {
        final Hold hold = holds.get(side);
        return hold != null && isLive(hold, now);
    }

    /** Who currently has {@code side}, if anybody. */
    public @NotNull Optional<UUID> holder(@NotNull SteerSide side, long now) {
        final Hold hold = holds.get(side);
        return hold != null && isLive(hold, now) ? Optional.of(hold.getHolder()) : Optional.empty();
    }

    /** Everyone with a hand on a control right now. */
    public @NotNull Set<UUID> holders(long now) {
        final Set<UUID> hands = new LinkedHashSet<>();
        for (SteerSide side : SteerSide.values()) {
            holder(side, now).ifPresent(hands::add);
        }
        return hands;
    }

    /**
     * What the crew is asking for: {@code +1} to starboard, {@code -1} to port, {@code 0} for nobody steering and
     * {@code 0} for both controls held — two people pulling opposite ways on one wheel is a stalemate, not a third
     * behaviour.
     */
    public int net(long now) {
        return (isActive(SteerSide.STARBOARD, now) ? 1 : 0) - (isActive(SteerSide.PORT, now) ? 1 : 0);
    }

    private static boolean isLive(@NotNull Hold hold, long now) {
        return now - hold.getAt() < GRACE_MILLIS;
    }
}
