package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.settler.crew.CrewService;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Opens the crew menus and carries out what they ask for. Both only work from inside the camp. */
@Singleton
@Getter(AccessLevel.PACKAGE)
public class CrewMenus {

    private final ConstructionService construction;
    private final CrewService crews;
    private final CrewRule rule;
    private final SettlerService settlers;
    private final StructureCatalogue catalogue;
    private final ProfessionRegistry professions;

    @Inject
    public CrewMenus(@NotNull ConstructionService construction, @NotNull CrewService crews, @NotNull CrewRule rule,
                     @NotNull SettlerService settlers, @NotNull StructureCatalogue catalogue,
                     @NotNull ProfessionRegistry professions) {
        this.construction = construction;
        this.crews = crews;
        this.rule = rule;
        this.settlers = settlers;
        this.catalogue = catalogue;
        this.professions = professions;
    }

    /** Every job running in the camp {@code player} stands in. */
    public void openJobs(@NotNull Player player) {
        construction.worksite(player.getWorld()).ifPresentOrElse(
                worksite -> new CrewJobsMenu(this, player, worksite).show(player),
                () -> tell(player, "clans.settler.crew.not_in_camp"));
    }

    /** The crew of the job on {@code structureId} in the camp {@code player} stands in. */
    public void openCrew(@NotNull Player player, @NotNull UUID structureId) {
        construction.worksite(player.getWorld())
                .flatMap(worksite -> worksite.getHolding().find(structureId)
                        .filter(structure -> structure.getJob() != null)
                        .map(structure -> new CrewMenu(this, player, worksite, structure)))
                .ifPresentOrElse(menu -> menu.show(player), () -> openJobs(player));
    }

    void join(@NotNull Player player, @NotNull UUID structureId, @NotNull UUID settlerId) {
        final SettlerResult result = crews.join(player, player.getWorld(), structureId, settlerId);
        if (!result.isSuccess() && result.getReason() != null) {
            UtilMessage.message(player, Translations.component("clans.prefix.settler"), result.getReason());
        }
        openCrew(player, structureId);
    }

    private void tell(@NotNull Player player, @NotNull String key) {
        UtilMessage.message(player, Translations.component("clans.prefix.settler"),
                Translations.component(key).color(NamedTextColor.GRAY));
    }
}
