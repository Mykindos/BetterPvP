package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/** Every job running in a camp, with how its crew stands. Clicking one opens its crew. */
public class CrewJobsMenu extends AbstractGui implements Windowed {

    CrewJobsMenu(@NotNull CrewMenus menus, @NotNull Player viewer, @NotNull ConstructionService.Worksite worksite) {
        super(9, 3);
        final long now = menus.getConstruction().now();
        final List<PlacedStructure> running = worksite.getHolding().getStructures().stream()
                .filter(structure -> structure.getJob() != null && !structure.getJob().isDone(now))
                .toList();

        if (running.isEmpty()) {
            setItem(13, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.settler.crew.no_jobs").color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int i = 0; i < running.size() && i < 27; i++) {
            final PlacedStructure structure = running.get(i);
            setItem(i, new SimpleItem(CrewMenu.summary(menus, worksite, structure, icon(menus, structure))
                    .action(ClickActions.ALL, Translations.component("clans.settler.crew.open"))
                    .build(), click -> menus.openCrew(click.getPlayer(), structure.getId())));
        }
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull Material icon(@NotNull CrewMenus menus, @NotNull PlacedStructure structure) {
        return menus.getCatalogue().find(structure.getType())
                .filter(CampStructure.class::isInstance)
                .map(type -> ((CampStructure) type).getIcon())
                .orElse(Material.BRICKS);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.settler.crew.jobs_title");
    }

    static @NotNull Component jobName(@NotNull Job job) {
        return Translations.component("clans.settler.crew.job." + job.getKind().name().toLowerCase(Locale.ROOT));
    }
}
