package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.settler.crew.CrewService;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Opens the crew menus and carries out what they ask for. Both only work from inside the camp. */
@Singleton
@Getter(AccessLevel.PACKAGE)
public class CrewMenus {

    @Getter(AccessLevel.NONE)
    private final ConstructionSites sites;
    private final StructureStatusTracker tracker;
    private final CrewService crews;
    private final CrewRule rule;
    private final SettlerService settlers;
    private final StructureCatalogue catalogue;

    @Inject
    public CrewMenus(@NotNull ConstructionSites sites, @NotNull StructureStatusTracker tracker,
                     @NotNull CrewService crews, @NotNull CrewRule rule,
                     @NotNull SettlerService settlers, @NotNull StructureCatalogue catalogue) {
        this.sites = sites;
        this.tracker = tracker;
        this.crews = crews;
        this.rule = rule;
        this.settlers = settlers;
        this.catalogue = catalogue;
    }

    /** Every job running in the camp {@code player} stands in. Back leads to {@code previous}. */
    public void openJobs(@NotNull Player player, @Nullable Windowed previous) {
        sites.worksite(player.getWorld()).ifPresentOrElse(
                worksite -> new CrewJobsMenu(this, player, worksite, previous).show(player),
                () -> tell(player, "clans.settler.crew.not_in_camp"));
    }

    /** The crew of the job on {@code structureId} in the camp {@code player} stands in. */
    public void openCrew(@NotNull Player player, @NotNull UUID structureId, @Nullable Windowed previous) {
        sites.worksite(player.getWorld())
                .flatMap(worksite -> worksite.getHolding().find(structureId)
                        .filter(structure -> structure.getJob() != null)
                        .map(structure -> new CrewMenu(this, player, worksite, structure, previous)))
                .ifPresentOrElse(menu -> menu.show(player), () -> openJobs(player, previous));
    }

    void join(@NotNull Player player, @NotNull UUID structureId, @NotNull UUID settlerId,
              @Nullable Windowed previous) {
        final SettlerResult result = crews.join(player, player.getWorld(), structureId, settlerId);
        if (!result.isSuccess() && result.getReason() != null) {
            UtilMessage.plain(player, result.getReason());
        }
        openCrew(player, structureId, previous);
    }

    private void tell(@NotNull Player player, @NotNull String key) {
        UtilMessage.plain(player, Translations.component(key).color(NamedTextColor.GRAY));
    }
}
