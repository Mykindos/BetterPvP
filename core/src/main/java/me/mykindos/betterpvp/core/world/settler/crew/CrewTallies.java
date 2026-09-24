package me.mykindos.betterpvp.core.world.settler.crew;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureClaimedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureRemovedEvent;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link CrewTally} for every job running on a loaded site, and the last finished one on each structure. Kept in
 * memory only, so a restart starts every tally again from where its job stands. A job that is cancelled drops its
 * tally, and a structure that is removed drops both.
 */
@BPvPListener
@Singleton
public class CrewTallies implements Listener {

    private final ConstructionService construction;
    private final SettlerService settlers;
    private final SiteInstances instances;
    private final CrewRule rule;

    private final Map<SiteKey, Map<UUID, Tracked>> tracked = new HashMap<>();

    @Inject
    public CrewTallies(@NotNull ConstructionService construction, @NotNull SettlerService settlers,
                       @NotNull SiteInstances instances, @NotNull CrewRule rule) {
        this.construction = construction;
        this.settlers = settlers;
        this.instances = instances;
        this.rule = rule;
    }

    /** The tally of the job running on each structure of {@code site}, then of the last finished one on each. */
    public @NotNull List<CrewTally> tallies(@NotNull SiteKey site) {
        final Map<UUID, Tracked> structures = tracked.getOrDefault(site, Map.of());
        final List<CrewTally> tallies = new ArrayList<>();
        structures.values().stream().map(Tracked::getRunning).filter(Objects::nonNull).forEach(tallies::add);
        structures.values().stream().map(Tracked::getLast).filter(Objects::nonNull).forEach(tallies::add);
        return tallies;
    }

    @UpdateEvent(delay = 5000)
    public void sweep() {
        final long now = construction.now();
        for (SiteInstance instance : new ArrayList<>(instances.all())) {
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (world == null) {
                continue;
            }
            construction.worksite(world).ifPresent(worksite -> {
                for (PlacedStructure structure : worksite.getHolding().getStructures()) {
                    track(worksite.getKey(), structure, structure.getJob(), now);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaimed(@NotNull StructureClaimedEvent event) {
        track(event.getSite(), event.getStructure(), event.getJob(), construction.now());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemoved(@NotNull StructureRemovedEvent event) {
        final Map<UUID, Tracked> structures = tracked.get(event.getSite());
        if (structures != null) {
            structures.remove(event.getStructure().getId());
        }
    }

    private void track(@NotNull SiteKey key, @NotNull PlacedStructure structure, @Nullable Job job,
                       long now) {
        final Map<UUID, Tracked> structures = tracked.computeIfAbsent(key, site -> new LinkedHashMap<>());
        final Tracked entry = structures.computeIfAbsent(structure.getId(), id -> new Tracked());
        if (job != entry.job) {
            entry.running = null;
            entry.job = null;
        }
        if (job == null) {
            return;
        }

        final boolean done = job.isDone(now);
        if (entry.running == null) {
            if (done || entry.job == job) {
                return;
            }
            entry.running = new CrewTally(structure, job, now);
            entry.job = job;
        }

        final Map<UUID, BuilderStats> crew = crew(key, structure, job);
        final CrewLimits limits = rule.limits(key);
        if (done) {
            entry.running.finish(crew, limits, job.progress(now), now);
            entry.last = entry.running;
            entry.running = null;
        } else {
            entry.running.sample(crew, limits, job.progress(now), job.isHeld(), now);
        }
    }

    /** What each working Builder on {@code job}'s crew brings, by settler id, leaving out anyone on strike. */
    private @NotNull Map<UUID, BuilderStats> crew(@NotNull SiteKey key, @NotNull PlacedStructure structure,
                                                  @NotNull Job job) {
        final SettlerSite owner = settlers.site(key).orElse(null);
        if (owner == null) {
            return Map.of();
        }
        final List<Settler> crew = rule.crew(key, job).stream()
                .filter(settler -> settler.getState() != SettlerState.STRIKING)
                .toList();
        final Map<UUID, BuilderStats> stats = new LinkedHashMap<>();
        for (Settler settler : crew) {
            final Optional<BuilderStats> brings = owner.builderStats(key, settler, structure, job, crew);
            brings.ifPresent(found -> stats.put(settler.getId(), found));
        }
        return stats;
    }

    /** The job a structure is being tallied for, which stays set once it finishes so it is not tallied again. */
    @Getter
    private static final class Tracked {
        private Job job;
        private CrewTally running;
        private CrewTally last;
    }
}
