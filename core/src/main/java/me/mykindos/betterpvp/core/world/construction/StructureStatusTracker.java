package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Keeps structure status moving with time: applies job rules, lets self-repairing structures come back, and announces
 * every status change with a {@link StructureStatusChangeEvent}.
 */
@Singleton
public class StructureStatusTracker {

    private final StructureCatalogue catalogue;
    private final LongSupplier clock;
    private final Map<UUID, StructureStatus> lastStatus = new ConcurrentHashMap<>();

    @Inject
    public StructureStatusTracker(@NotNull StructureCatalogue catalogue) {
        this(catalogue, System::currentTimeMillis);
    }

    StructureStatusTracker(@NotNull StructureCatalogue catalogue, @NotNull LongSupplier clock) {
        this.catalogue = catalogue;
        this.clock = clock;
    }

    public long now() {
        return clock.getAsLong();
    }

    /** Applies job rules and announces any status that changed, for every structure on one site. */
    public void refresh(@NotNull Worksite worksite) {
        for (PlacedStructure structure : worksite.getHolding().getStructures()) {
            final boolean ruled = applyRules(worksite, structure);
            final boolean repaired = repairedItself(structure);
            if (ruled || repaired) {
                worksite.getSite().changed(worksite.getKey());
            }
            publish(worksite, structure);
        }
    }

    /** Starts tracking a new structure from its current status, without announcing it. */
    void track(@NotNull PlacedStructure structure) {
        lastStatus.put(structure.getId(), structure.status(now()));
    }

    void forget(@NotNull PlacedStructure structure) {
        lastStatus.remove(structure.getId());
    }

    /**
     * Rules govern running jobs only, so a finished one waits to be claimed whatever changes around it.
     *
     * @return whether anything about the job changed
     */
    boolean applyRules(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        final Job job = structure.getJob();
        final long now = now();
        if (job == null || job.isDone(now)) {
            return false;
        }

        boolean changed = false;
        double rate = 1.0;
        for (JobRule rule : worksite.getSite().jobRules()) {
            final boolean holds = rule.holds(worksite.getKey(), structure, job);
            if (holds && !job.getHolds().contains(rule.id())) {
                job.hold(rule.id(), now);
                changed = true;
            } else if (!holds && job.getHolds().contains(rule.id())) {
                job.release(rule.id(), now);
                changed = true;
            }
            rate *= rule.rate(worksite.getKey(), structure, job);
        }
        if (Math.abs(rate - job.getRate()) > 1e-9) {
            job.setRate(rate, now);
            changed = true;
        }
        return changed;
    }

    void publish(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        final StructureStatus status = structure.status(now());
        final StructureStatus previous = lastStatus.put(structure.getId(), status);
        if (previous != null && previous != status) {
            UtilServer.callEvent(new StructureStatusChangeEvent(worksite.getKey(), structure, previous, status));
        }
    }

    static void repaired(@NotNull PlacedStructure structure) {
        structure.setCondition(StructureCondition.ACTIVE);
        structure.setDisabledAt(null);
    }

    private boolean repairedItself(@NotNull PlacedStructure structure) {
        final Long disabledAt = structure.getDisabledAt();
        if (structure.getCondition() != StructureCondition.DISABLED || disabledAt == null
                || structure.getJob() != null) {
            return false;
        }
        final Optional<StructureType> type = catalogue.find(structure.getType());
        if (type.isEmpty() || !type.get().getFlags().isSelfRepairing()
                || now() - disabledAt < type.get().getRepairTime().toMillis()) {
            return false;
        }
        repaired(structure);
        return true;
    }
}
