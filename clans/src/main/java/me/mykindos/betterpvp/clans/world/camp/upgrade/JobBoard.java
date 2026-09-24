package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.menu.StructureMenus;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Workshop upgrade: every job in the camp on one page, with its crew and when it finishes. Clicking one opens what can
 * be done to its structure.
 */
@BPvPListener
@Singleton
@Getter(AccessLevel.PACKAGE)
public class JobBoard implements Listener {

    public static final String ID = "job_board";

    private final CampUpgrades upgrades;
    private final CampStore store;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final StructureMenus structureMenus;
    private final CrewRule crews;

    @Inject
    public JobBoard(@NotNull CampUpgrades upgrades, @NotNull CampStore store, @NotNull ConstructionService construction,
                    @NotNull StructureCatalogue catalogue, @NotNull StructureMenus structureMenus,
                    @NotNull CrewRule crews) {
        this.upgrades = upgrades;
        this.store = store;
        this.construction = construction;
        this.catalogue = catalogue;
        this.structureMenus = structureMenus;
        this.crews = crews;
        upgrades.declare(CampStructures.WORKSHOP, ID, 1);
        upgrades.page(ID, (player, camp, structure, previous) -> open(player, camp, previous));
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.WORKSHOP, ID);
    }

    /** Opens the board for camp {@code camp}, if its Workshop has one working. */
    public void open(@NotNull Player player, @NotNull SiteKey camp, @Nullable Windowed previous) {
        if (!isActive(camp)) {
            UtilMessage.message(player, Translations.component("clans.prefix.camp"),
                    Translations.component("clans.camp.upgrade.job_board.inactive").color(NamedTextColor.RED));
            return;
        }
        new JobBoardMenu(this, camp, previous).show(player);
    }

    @NotNull List<Entry> entries(@NotNull SiteKey camp) {
        return store.cached(camp.getOwnerId())
                .map(Camp::getHolding)
                .map(holding -> entries(holding, construction.now()))
                .orElse(List.of());
    }

    /** Every structure in {@code holding} with a job: ready to claim first, then running soonest done, then paused. */
    static @NotNull List<Entry> entries(@NotNull Holding holding, long now) {
        final List<Entry> entries = new ArrayList<>();
        for (PlacedStructure structure : holding.getStructures()) {
            final Job job = structure.getJob();
            if (job == null) {
                continue;
            }
            final State state = job.isDone(now) ? State.READY : job.isHeld() ? State.PAUSED : State.RUNNING;
            entries.add(new Entry(structure, job, state, state == State.RUNNING ? job.remainingMillis(now) : 0));
        }
        entries.sort(Comparator.comparingInt((Entry entry) -> entry.getState().ordinal())
                .thenComparingLong(Entry::getRemainingMillis));
        return entries;
    }

    /** Where a job stands, in the order the board lists them. */
    enum State {
        READY,
        RUNNING,
        PAUSED
    }

    /** One job on the board. */
    @Value
    static class Entry {
        PlacedStructure structure;
        Job job;
        State state;
        /** How long a running job has left at its pace, {@link Long#MAX_VALUE} if it would never finish, else 0. */
        long remainingMillis;
    }
}
