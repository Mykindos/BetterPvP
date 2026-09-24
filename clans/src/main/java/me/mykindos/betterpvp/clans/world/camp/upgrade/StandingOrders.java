package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.settler.crew.CrewService;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Great Hall upgrade: free Builders join the crews of jobs held for want of one, the longest held first, until each
 * crew has the Workforce its job needs or no Builder is left. A Builder is free while it has no assignment and is not
 * on strike. Checked every few seconds on the server holding the camp's world.
 */
@BPvPListener
@Singleton
public class StandingOrders implements Listener {

    public static final String ID = "standing_orders";

    private final CampUpgrades upgrades;
    private final ConstructionService construction;
    private final SettlerService settlers;
    private final CrewService crews;
    private final CrewRule rule;
    private final SiteInstances instances;

    @Inject
    public StandingOrders(@NotNull CampUpgrades upgrades, @NotNull ConstructionService construction,
                          @NotNull SettlerService settlers, @NotNull CrewService crews, @NotNull CrewRule rule,
                          @NotNull SiteInstances instances) {
        this.upgrades = upgrades;
        this.construction = construction;
        this.settlers = settlers;
        this.crews = crews;
        this.rule = rule;
        this.instances = instances;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 1);
    }

    @UpdateEvent(delay = 5000)
    public void tick() {
        for (SiteInstance instance : new ArrayList<>(instances.all())) {
            if (!instance.getKey().getSiteId().equals(Camps.SITE_ID)) {
                continue;
            }
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (world != null) {
                construction.worksite(world).ifPresent(worksite -> staff(worksite.getKey(), worksite.getHolding(),
                        (structure, settler) -> crews.enlist(worksite, structure, settler)));
            }
        }
    }

    /**
     * Puts free Builders on the crews of the jobs in {@code holding} that wait for one.
     *
     * @param enlist puts a settler on the crew of a structure's job
     */
    void staff(@NotNull SiteKey key, @NotNull Holding holding,
               @NotNull BiFunction<UUID, UUID, SettlerResult> enlist) {
        if (!upgrades.has(key, CampConstruction.GREAT_HALL, ID)) {
            return;
        }
        final Roster roster = settlers.roster(key).orElse(null);
        if (roster == null) {
            return;
        }
        final List<Settler> free = new ArrayList<>(roster.getSettlers().stream()
                .filter(settler -> settler.getState() == SettlerState.IDLE && crews.isFree(settler))
                .toList());
        if (free.isEmpty()) {
            return;
        }

        final long now = construction.now();
        final List<PlacedStructure> waiting = holding.getStructures().stream()
                .filter(structure -> structure.getJob() != null && !structure.getJob().isDone(now))
                .filter(structure -> rule.holds(key, structure, structure.getJob()))
                .sorted(Comparator.comparingLong(structure -> structure.getJob().getCheckpointAt()))
                .toList();
        for (PlacedStructure structure : waiting) {
            final Job job = structure.getJob();
            final Iterator<Settler> candidates = free.iterator();
            while (candidates.hasNext() && rule.holds(key, structure, job)) {
                final Settler settler = candidates.next();
                if (enlist.apply(structure.getId(), settler.getId()).isSuccess()) {
                    candidates.remove();
                }
            }
            if (free.isEmpty()) {
                return;
            }
        }
    }
}
