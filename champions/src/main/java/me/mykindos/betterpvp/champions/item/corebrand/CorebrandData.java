package me.mykindos.betterpvp.champions.item.corebrand;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;

/**
 * Per-player Corebrand state: how much fuel they have banked, and whether they are currently in Cascade.
 * <p>
 * Fuel is a 0-1 fraction of a full bar. It is earned outside of Cascade and spent inside it, so the two
 * phases never compete for the same tick.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class CorebrandData {

    private final Gamer gamer;

    private float fuel;
    private boolean cascading;
    private long cascadeStart;
    private long lastAction;
    private long nextTickSound;
    private int ticks;
    private State state = State.DORMANT;
    private boolean markForRemoval;

    /**
     * Pays for an action. There is no affordability check — a cost larger than the tank simply empties it,
     * and the empty tank is what ends Cascade on the following tick.
     */
    public void spend(double cost) {
        fuel = (float) Math.max(0, fuel - cost);
        lastAction = System.currentTimeMillis();
    }

    public void gain(double amount) {
        fuel = (float) Math.min(1.0, fuel + amount);
        lastAction = System.currentTimeMillis();
    }

    public void decay(double amount) {
        fuel = (float) Math.max(0, fuel - amount);
    }

    public boolean isFull() {
        return fuel >= 1.0f;
    }

    /**
     * The two looks the blade can wear. It lights up at a full core and stays lit for the whole of Cascade,
     * so a full bar and an active window read as the same weapon to everyone watching.
     */
    public enum State {
        DORMANT,
        CHARGED
    }
}
