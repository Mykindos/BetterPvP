package me.mykindos.betterpvp.core.world.construction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Work under way on a structure, timed in real time.
 * <p>
 * Progress is kept as a checkpoint: how far along it was at {@link #checkpointAt}, and how fast it has gone since. So it
 * can be read at any moment without anything ticking, which is what lets a job carry on while nobody is there and its
 * world is not even loaded. Anything that changes the pace (a hold, a new rate) takes a checkpoint first, so time
 * already spent is kept at the old pace.
 */
@Data
@NoArgsConstructor
public class Job {

    private JobKind kind;
    private long durationMillis;
    /** Progress from 0 to 1 at {@link #checkpointAt}. */
    private double progress;
    private long checkpointAt;
    /** How many times faster than its base duration it runs. */
    private double rate = 1.0;
    /** Why it is paused, if it is. It runs only while this is empty. */
    private Set<String> holds = new LinkedHashSet<>();
    /** What was paid for it, which is what cancelling hands back. */
    private ResourceCost spent = ResourceCost.NONE;
    /** Whoever is working on it. */
    private List<UUID> staff = new ArrayList<>();
    /** The version the structure will be at once this job is claimed. */
    private int targetVersion;
    /** Where a move is taking the structure. Null for anything but a move. */
    private @Nullable StructurePosition target;

    public static @NotNull Job start(@NotNull JobKind kind, @NotNull Duration duration, @NotNull ResourceCost spent,
                                     int targetVersion, long now) {
        final Job job = new Job();
        job.kind = kind;
        job.durationMillis = Math.max(0, duration.toMillis());
        job.checkpointAt = now;
        job.spent = spent;
        job.targetVersion = targetVersion;
        return job;
    }

    /** How far along it is at {@code now}, from 0 to 1. */
    public double progress(long now) {
        if (durationMillis <= 0) {
            return 1.0;
        }
        double current = progress;
        if (holds.isEmpty()) {
            current += (double) Math.max(0, now - checkpointAt) * rate / durationMillis;
        }
        return Math.clamp(current, 0.0, 1.0);
    }

    public boolean isDone(long now) {
        return progress(now) >= 1.0;
    }

    @JsonIgnore
    public boolean isHeld() {
        return !holds.isEmpty();
    }

    /** How long is left at the current pace, as if it were running. {@link Long#MAX_VALUE} if it would never finish. */
    public long remainingMillis(long now) {
        final double left = 1.0 - progress(now);
        if (left <= 0) {
            return 0;
        }
        if (rate <= 0) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(left * durationMillis / rate);
    }

    /** Pauses it for {@code reason}. Several reasons can hold it at once, and it runs again only once all are gone. */
    public void hold(@NotNull String reason, long now) {
        checkpoint(now);
        holds.add(reason);
    }

    public void release(@NotNull String reason, long now) {
        checkpoint(now);
        holds.remove(reason);
    }

    public void setRate(double rate, long now) {
        checkpoint(now);
        this.rate = Math.max(0, rate);
    }

    private void checkpoint(long now) {
        progress = progress(now);
        checkpointAt = now;
    }
}
