package me.mykindos.betterpvp.core.utilities.model.display;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Getter
public class TimedDisplayObject<T> extends DisplayObject<T> implements ITimedDisplay {

    private final double seconds;
    private final boolean waitToExpire;
    private long startTime = -1;

    /**
     * @param seconds      The amount of seconds to show the component for.
     * @param waitToExpire Whether to wait for the component to show first before allowing it to start expiring.
     * @param provider     The component to show. Return null to not display anything and skip this component.
     */
    public TimedDisplayObject(double seconds, boolean waitToExpire, @NotNull Function<Gamer, T> provider) {
        super(provider);
        this.seconds = seconds;
        this.waitToExpire = waitToExpire;
    }

    @Override
    public long getRemaining() {
        long millisDuration = (long) (seconds * 1000);
        return (startTime + millisDuration) - System.currentTimeMillis();
    }

    @Override
    public boolean hasStarted() {
        return startTime != -1;
    }

    /**
     * Returns {@code true} if this object has been explicitly invalidated, or if its
     * display duration has elapsed. The time-based check ensures correctness when this
     * object is read from a different thread than the one that started the timer — the
     * Bukkit-scheduler-set {@code invalid} flag has no cross-thread visibility guarantee.
     */
    @Override
    public boolean isInvalid() {
        return super.isInvalid() || (hasStarted() && getRemaining() <= 0);
    }

    @Override
    public void startTime() {
        if (hasStarted()) {
            return;
        }

        startTime = System.currentTimeMillis();
    }

}
