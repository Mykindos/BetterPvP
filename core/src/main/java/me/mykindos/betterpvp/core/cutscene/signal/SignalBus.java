package me.mykindos.betterpvp.core.cutscene.signal;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The signals one cutscene session has seen, and when.
 * <p>
 * Firings are recorded with the tick they happened on rather than as a set of names, because "has this happened" is the
 * wrong question - a beat waiting on {@code node:greet} must not be satisfied by the {@code node:greet} that fired
 * before the beat started. Asking {@link #firedSince} instead makes every wait mean "from now on", which is what an
 * author writing {@code until(node("greet"))} halfway down a timeline expects.
 */
public class SignalBus {

    private final Map<String, Integer> lastFired = new ConcurrentHashMap<>();

    public void emit(@NotNull Signal signal, int tick) {
        lastFired.put(signal.getKey(), tick);
    }

    /**
     * @return true if {@code signal} has fired on or after {@code tick}
     */
    public boolean firedSince(@NotNull Signal signal, int tick) {
        final Integer fired = lastFired.get(signal.getKey());
        return fired != null && fired >= tick;
    }

    /** Whether {@code signal} has ever fired in this session, regardless of when. */
    public boolean everFired(@NotNull Signal signal) {
        return lastFired.containsKey(signal.getKey());
    }

    public void clear() {
        lastFired.clear();
    }
}
