package me.mykindos.betterpvp.core.cutscene.camera;

import me.mykindos.betterpvp.core.cutscene.CutsceneSession;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * What has to become true before the camera leaves a beat.
 * <p>
 * Only consulted once the camera has finished travelling, so every condition is written from the moment of arrival
 * rather than from the moment the beat was queued - a beat cannot end before the shot it describes is on screen.
 */
@FunctionalInterface
public interface BeatAdvance {

    /**
     * @param ticksSinceArrival ticks since the camera finished travelling to this beat, {@code 0} on the arrival tick
     */
    boolean isSatisfied(@NotNull CutsceneSession session, int ticksSinceArrival);

    /** Leave after dwelling for {@code ticks} once the camera has arrived. {@code 0} leaves immediately. */
    static @NotNull BeatAdvance hold(int ticks) {
        return (session, ticksSinceArrival) -> ticksSinceArrival >= ticks;
    }

    /** Never leaves on its own - the beat ends only by a skip or by the cutscene being stopped. */
    static @NotNull BeatAdvance never() {
        return (session, ticksSinceArrival) -> false;
    }

    /**
     * Leave when {@code signal} fires, counting firings from the moment this beat <em>started</em> - not from when the
     * camera arrived.
     * <p>
     * The distinction is load-bearing. A long travel can easily outlast the dialogue it is paced against: if the
     * player answers while the camera is still moving, a signal counted only from arrival would already have passed
     * and the beat would wait for it forever. Counting from the start means the camera finishes its move and then
     * moves on immediately, which is both correct and what it looks like it should do.
     */
    static @NotNull BeatAdvance signal(@NotNull Signal signal) {
        return (session, ticksSinceArrival) -> session.getSignals().firedSince(signal, session.getBeatStartTick());
    }

    /** Leave when {@code condition} holds for the viewer. Evaluated every tick, so keep it cheap. */
    static @NotNull BeatAdvance when(@NotNull Predicate<Player> condition) {
        return (session, ticksSinceArrival) -> {
            final Player player = session.getPlayer();
            return player != null && condition.test(player);
        };
    }

    /** Leave as soon as any of {@code conditions} is satisfied - the way a beat waits on a branching conversation. */
    static @NotNull BeatAdvance anyOf(@NotNull BeatAdvance @NotNull ... conditions) {
        return (session, ticksSinceArrival) -> {
            for (BeatAdvance condition : conditions) {
                if (condition.isSatisfied(session, ticksSinceArrival)) {
                    return true;
                }
            }
            return false;
        };
    }
}
