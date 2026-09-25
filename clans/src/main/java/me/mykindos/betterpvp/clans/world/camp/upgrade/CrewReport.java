package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.crew.CrewTallies;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Workshop upgrade: for every running job, and the last finished one on each structure, how much of the work each
 * Builder did and how much of its Speed the job could not use. The numbers are core's {@link CrewTallies}.
 */
@BPvPListener
@Singleton
@Getter(AccessLevel.PACKAGE)
public class CrewReport implements Listener {

    public static final String ID = "crew_report";

    private final CampUpgrades upgrades;
    private final CrewTallies tallies;
    private final SettlerService settlers;
    private final StructureCatalogue catalogue;

    @Inject
    public CrewReport(@NotNull CampUpgrades upgrades, @NotNull CrewTallies tallies, @NotNull SettlerService settlers,
                      @NotNull StructureCatalogue catalogue) {
        this.upgrades = upgrades;
        this.tallies = tallies;
        this.settlers = settlers;
        this.catalogue = catalogue;
        upgrades.declare(CampStructures.WORKSHOP, ID, 3);
        upgrades.page(ID, (player, camp, structure, previous) -> open(player, camp, previous));
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.WORKSHOP, ID);
    }

    /** Opens the report for camp {@code camp}, if its Workshop has one working. */
    public void open(@NotNull Player player, @NotNull SiteKey camp, @Nullable Windowed previous) {
        if (!isActive(camp)) {
            UtilMessage.plain(player, Translations.component("clans.camp.upgrade.crew_report.inactive").color(NamedTextColor.RED));
            return;
        }
        new CrewReportMenu(this, camp, previous).show(player);
    }
}
