package me.mykindos.betterpvp.core.world.settler.crew;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureRemovedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureStatusChangeEvent;
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.construction.Worksite;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerAssignedEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.WorkplaceKind;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Puts Builders on crews and lets them go. A Builder joins a job that is still running and stays on it until it ends,
 * which is when the job is finished, cancelled or its structure is gone, or until it is taken off. A Builder's
 * assignment is the id of the structure it works on, and it is on that job's crew only while it stays assigned there.
 */
@BPvPListener
@Singleton
public class CrewService implements Listener {

    private final ConstructionSites sites;
    private final StructureStatusTracker tracker;
    private final SettlerService settlers;
    private final ProfessionRegistry professions;
    private final SiteInstances instances;
    private final CrewRule rule;

    @Inject
    public CrewService(@NotNull ConstructionSites sites, @NotNull StructureStatusTracker tracker,
                       @NotNull SettlerService settlers,
                       @NotNull ProfessionRegistry professions, @NotNull SiteInstances instances,
                       @NotNull CrewRule rule) {
        this.sites = sites;
        this.tracker = tracker;
        this.settlers = settlers;
        this.professions = professions;
        this.instances = instances;
        this.rule = rule;
    }

    /** Whether {@code settler} is a Builder with nothing to do, so could join a crew. */
    public boolean isFree(@NotNull Settler settler) {
        return settler.getAssignment() == null && builds(settler);
    }

    public boolean builds(@NotNull Settler settler) {
        return settler.getProfession() != null && professions.find(settler.getProfession())
                .map(Profession::getWorkplaceKind)
                .filter(kind -> kind == WorkplaceKind.CONSTRUCTION)
                .isPresent();
    }

    /**
     * Puts {@code settlerId} on the crew of the job running on {@code structureId}, if {@code player} may and the crew
     * has room for it.
     */
    public @NotNull SettlerResult join(@NotNull Player player, @NotNull World world, @NotNull UUID structureId,
                                       @NotNull UUID settlerId) {
        final Worksite worksite = sites.worksite(world).orElse(null);
        final SettlerSite site = worksite == null ? null : settlers.site(worksite.getKey()).orElse(null);
        if (worksite == null || site == null) {
            return SettlerResult.refused("core.settler.not_loaded");
        }
        if (!site.allows(player, worksite.getKey(), SettlerAction.ASSIGN)) {
            return SettlerResult.refused("core.settler.not_allowed");
        }
        return enlist(worksite, structureId, settlerId);
    }

    /** Puts {@code settlerId} on the crew of the job running on {@code structureId}, if the crew has room for it. */
    public @NotNull SettlerResult enlist(@NotNull Worksite worksite, @NotNull UUID structureId,
                                         @NotNull UUID settlerId) {
        final SiteKey key = worksite.getKey();
        final SettlerSite site = settlers.site(key).orElse(null);
        final Roster roster = settlers.roster(key).orElse(null);
        if (site == null || roster == null) {
            return SettlerResult.refused("core.settler.not_loaded");
        }
        final PlacedStructure structure = worksite.getHolding().find(structureId).orElse(null);
        final Job job = structure == null ? null : structure.getJob();
        if (job == null || job.isDone(tracker.now())) {
            return SettlerResult.refused("core.settler.crew.no_job");
        }
        final Settler settler = roster.find(settlerId).orElse(null);
        if (settler == null) {
            return SettlerResult.refused("core.settler.not_found");
        }
        if (!builds(settler)) {
            return SettlerResult.refused("core.settler.crew.not_builder");
        }
        if (job.getStaff().contains(settlerId)) {
            return SettlerResult.refused("core.settler.crew.already_on");
        }
        if (settler.getAssignment() != null) {
            return SettlerResult.refused("core.settler.crew.busy");
        }

        final CrewLimits limits = site.crewLimits(key);
        final List<Settler> crew = rule.crew(key, job);
        if (crew.size() >= limits.getMaxSize()) {
            return SettlerResult.refused("core.settler.crew.full", Component.text(limits.getMaxSize()));
        }
        final Integer rarityCap = limits.getPerRarity().get(settler.getRarity());
        if (rarityCap != null && crew.stream().filter(member -> member.getRarity() == settler.getRarity()).count() >= rarityCap) {
            return SettlerResult.refused("core.settler.crew.rarity_full", settler.getRarity().displayName(),
                    Component.text(rarityCap));
        }

        final SettlerResult assigned = settlers.assign(key, settlerId, structureId.toString());
        if (!assigned.isSuccess()) {
            return assigned;
        }
        job.getStaff().removeIf(id -> crew.stream().noneMatch(member -> member.getId().equals(id)));
        job.getStaff().add(settlerId);
        worksite.getSite().changed(key);
        tracker.refresh(worksite);
        return assigned;
    }

    /**
     * A Builder taken off or moved from a structure leaves that job's crew at once, which may pause the job. A job whose
     * time is up keeps its crew until it is let go, so everyone who saw it through is credited.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAssigned(@NotNull SettlerAssignedEvent event) {
        final UUID structureId = event.getPrevious() == null ? null : parse(event.getPrevious()).orElse(null);
        if (structureId == null) {
            return;
        }
        worksites(event.getSite(), worksite -> worksite.getHolding().find(structureId)
                .map(PlacedStructure::getJob)
                .filter(job -> !job.isDone(tracker.now()) && job.getStaff().remove(event.getSettler().getId()))
                .ifPresent(job -> {
                    worksite.getSite().changed(worksite.getKey());
                    tracker.refresh(worksite);
                }));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatusChange(@NotNull StructureStatusChangeEvent event) {
        release(event.getSite());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemoved(@NotNull StructureRemovedEvent event) {
        release(event.getSite());
    }

    /** Catches jobs that finished while their world was closed, and crews whose members have left. */
    @UpdateEvent(delay = 5000)
    public void sweep() {
        for (SiteInstance instance : new ArrayList<>(instances.all())) {
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (world != null) {
                sites.worksite(world).ifPresent(worksite -> release(worksite.getKey(), worksite.getHolding()));
            }
        }
    }

    private void release(@NotNull SiteKey key) {
        worksites(key, worksite -> release(key, worksite.getHolding()));
    }

    /** Runs {@code action} on the worksite of each open instance of {@code key}. */
    private void worksites(@NotNull SiteKey key, @NotNull Consumer<Worksite> action) {
        for (SiteInstance instance : instances.forKey(key)) {
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (world != null) {
                sites.worksite(world).ifPresent(action);
            }
        }
    }

    /**
     * Lets go of every Builder whose job has ended. A crew that saw its job through is counted once, before it is let
     * go, so its site can reward it.
     */
    void release(@NotNull SiteKey key, @NotNull Holding holding) {
        final SettlerSite site = settlers.site(key).orElse(null);
        final Roster roster = settlers.roster(key).orElse(null);
        if (site == null || roster == null) {
            return;
        }
        final long now = tracker.now();
        boolean changed = false;

        for (PlacedStructure structure : holding.getStructures()) {
            final Job job = structure.getJob();
            if (job == null || job.getStaff().isEmpty()) {
                continue;
            }
            changed |= job.getStaff().removeIf(id -> roster.find(id).isEmpty());
            if (job.isDone(now) && !job.getStaff().isEmpty()) {
                final List<Settler> crew = rule.crew(key, job);
                crew.forEach(member -> member.setJobsFinished(member.getJobsFinished() + 1));
                site.crewFinished(key, structure, job, crew);
                job.getStaff().clear();
                changed = true;
            }
        }

        for (Settler settler : List.copyOf(roster.getSettlers())) {
            if (settler.getAssignment() == null || !builds(settler)) {
                continue;
            }
            final Optional<Job> job = parse(settler.getAssignment())
                    .flatMap(holding::find)
                    .map(PlacedStructure::getJob);
            if (job.isEmpty() || job.get().isDone(now) || !job.get().getStaff().contains(settler.getId())) {
                settlers.unassign(key, settler.getId());
            }
        }
        if (changed) {
            site.changed(key);
        }
    }

    private static @NotNull Optional<UUID> parse(@NotNull String assignment) {
        try {
            return Optional.of(UUID.fromString(assignment));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
