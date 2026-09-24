package me.mykindos.betterpvp.core.world.settler.crew;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who did how much of one job. It is sampled now and then: the progress the job made since the last sample is split
 * between the Builders on its crew by what each adds to the crew's speed ({@link CrewSpeed#contributions}), and the
 * Speed each brought that the job could not use ({@link CrewSpeed#wasted}) is added up over the time it ran. Time the
 * job spent held counts for neither.
 */
@Getter
public class CrewTally {

    private final UUID structure;
    /** The type id of the structure. */
    private final String structureType;
    private final JobKind kind;
    private final int targetStage;
    private final @Nullable String upgrade;
    @Getter(AccessLevel.NONE)
    private final Map<UUID, Share> shares = new LinkedHashMap<>();
    private boolean finished;
    @Getter(AccessLevel.NONE)
    private double progress;
    @Getter(AccessLevel.NONE)
    private long sampledAt;

    /** Starts counting {@code job} on {@code structure} from how far along it is at {@code now}. */
    public CrewTally(@NotNull PlacedStructure structure, @NotNull Job job, long now) {
        this.structure = structure.getId();
        this.structureType = structure.getType();
        this.kind = job.getKind();
        this.targetStage = job.getTargetStage();
        this.upgrade = job.getUpgrade();
        this.progress = job.progress(now);
        this.sampledAt = now;
    }

    /**
     * Counts the time since the last sample.
     *
     * @param crew     what each working Builder on the crew brings, by settler id
     * @param progress how far along the job is now, from 0 to 1
     * @param held     whether the job is held, so nobody worked on it
     */
    public void sample(@NotNull Map<UUID, BuilderStats> crew, @NotNull CrewLimits limits, double progress,
                       boolean held, long now) {
        final long elapsed = Math.max(0, now - sampledAt);
        final double made = Math.max(0, progress - this.progress);
        this.progress = Math.max(this.progress, progress);
        this.sampledAt = now;
        crew.keySet().forEach(id -> shares.computeIfAbsent(id, key -> new Share()));
        if (held) {
            return;
        }
        if (crew.isEmpty()) {
            if (progress >= 1.0) {
                splitByWork(made);
            }
            return;
        }

        final List<UUID> ids = new ArrayList<>(crew.keySet());
        final List<BuilderStats> stats = ids.stream().map(crew::get).toList();
        final double[] contributions = CrewSpeed.contributions(stats, limits);
        final double[] wasted = CrewSpeed.wasted(stats, limits);
        double total = 0;
        for (double contribution : contributions) {
            total += contribution;
        }
        for (int i = 0; i < ids.size(); i++) {
            final Share share = shares.get(ids.get(i));
            if (total > 0) {
                share.work += made * contributions[i] / total;
            }
            share.wastedSpeedMillis += wasted[i] * elapsed;
            share.millis += elapsed;
        }
    }

    /** Stops counting, with the job at {@code progress} at {@code now}. */
    public void finish(@NotNull Map<UUID, BuilderStats> crew, @NotNull CrewLimits limits, double progress, long now) {
        sample(crew, limits, progress, false, now);
        finished = true;
    }

    /** Every Builder that has been on the crew while it was counted, in the order they joined. */
    public @NotNull Map<UUID, Share> getShares() {
        return Collections.unmodifiableMap(shares);
    }

    /** How much of the job was counted, from 0 to 1. */
    public double totalWork() {
        return shares.values().stream().mapToDouble(Share::getWork).sum();
    }

    /** The share of the counted work {@code settler} did, from 0 to 1. */
    public double shareOf(@NotNull UUID settler) {
        final Share share = shares.get(settler);
        final double total = totalWork();
        return share == null || total <= 0 ? 0 : share.work / total;
    }

    /** Progress made while nobody was left on the crew goes to those who did the rest, in the same proportions. */
    private void splitByWork(double made) {
        final double total = totalWork();
        if (made <= 0 || total <= 0) {
            return;
        }
        shares.values().forEach(share -> share.work += made * share.work / total);
    }

    /** One Builder's part in a job. */
    @Data
    public static class Share {
        /** How much of the job it did, from 0 to 1. */
        private double work;
        /** Speed it brought that the job could not use, times how long, in milliseconds. */
        private double wastedSpeedMillis;
        /** How long it was on the crew while the job ran. */
        private long millis;

        /** The Speed it brought that the job could not use, on average over the time it worked. */
        public double wastedSpeed() {
            return millis <= 0 ? 0 : wastedSpeedMillis / millis;
        }
    }
}
