package me.mykindos.betterpvp.core.item.component.impl.temper;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.temper.TemperProfile;
import org.jetbrains.annotations.NotNull;

/**
 * A pool an item spends to run its own abilities, in place of the wielder's energy. It drains while
 * an ability is held, pauses, then refills on its own.
 * <p>
 * The {@link TemperProfile} is the item's definition and is shared by reference with every instance
 * of that item; the three value fields are per-instance state and are persisted to the stack.
 * <p>
 * Recovery is resolved two different ways, which is what {@link #temperTime} exists for. An
 * unconditional profile recovers by the clock, so {@link #temperAt(long)} can extrapolate from the
 * stored value at any later moment — including for a stack sitting in a chest. A profile whose
 * recovery depends on the wielder cannot be extrapolated, because the condition may have come and
 * gone; {@link me.mykindos.betterpvp.core.item.temper.TemperService} integrates it tick by tick
 * through {@link #recover(double, long)} instead. Storing when the value was last measured keeps
 * those two paths from ever counting the same second twice.
 */
@Getter
public class TemperComponent extends AbstractItemComponent {

    private final @NotNull TemperProfile profile;

    /** The last measured temper, from 0 (spent) to 1 (full). */
    private double temper = 1;
    /** When {@link #temper} was measured, in epoch millis. */
    @Setter private long temperTime;
    /** When the item was last drained, in epoch millis. Gates the recovery delay. */
    @Setter private long lastDrainTime;

    public TemperComponent(@NotNull TemperProfile profile) {
        super("temper");
        this.profile = profile;
    }

    public void setTemper(double temper) {
        this.temper = Math.clamp(temper, 0, 1);
    }

    /**
     * The temper this item holds right now, including recovery accrued since it was last measured.
     *
     * @param now the current epoch millis
     */
    public double temperAt(long now) {
        if (temper >= 1 || !profile.recoversWhileStowed()) {
            return temper;
        }

        final long recoveryStart = Math.max(temperTime, lastDrainTime + (long) (profile.getRecoveryDelay() * 1000));
        if (now <= recoveryStart) {
            return temper;
        }
        return Math.min(1, temper + (now - recoveryStart) / 1000d * profile.recoveryPerSecond());
    }

    public boolean isSpentAt(long now) {
        return temperAt(now) <= 0;
    }

    /**
     * Settles the bar up to {@code now} and then spends from it, restarting the recovery delay.
     *
     * @param amount fraction of a full bar to spend
     * @param now    the current epoch millis
     */
    public void drain(double amount, long now) {
        setTemper(temperAt(now) - amount);
        this.temperTime = now;
        this.lastDrainTime = now;
    }

    /**
     * Adds recovery measured by the caller, for profiles that cannot be extrapolated by the clock.
     *
     * @param amount fraction of a full bar recovered
     * @param now    the current epoch millis
     */
    public void recover(double amount, long now) {
        setTemper(temper + amount);
        this.temperTime = now;
    }

    @Override
    public TemperComponent copy() {
        final TemperComponent copy = new TemperComponent(profile);
        copy.setTemper(temper);
        copy.setTemperTime(temperTime);
        copy.setLastDrainTime(lastDrainTime);
        return copy;
    }

}
