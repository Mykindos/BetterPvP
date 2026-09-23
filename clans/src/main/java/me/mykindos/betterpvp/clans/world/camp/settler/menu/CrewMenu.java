package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * One job's crew: the Workforce it needs against what the crew brings, how fast it runs, who is on it, and the free
 * Builders who could join. Builders stay on a crew until its job ends.
 */
public class CrewMenu extends AbstractGui implements Windowed {

    private static final int FIRST_FREE_SLOT = 27;
    private static final int FREE_SLOTS = 18;

    private final CrewMenus menus;
    private final Player viewer;
    private final SiteKey key;
    private final PlacedStructure structure;
    private final Job job;
    private final SettlerSite site;

    CrewMenu(@NotNull CrewMenus menus, @NotNull Player viewer, @NotNull ConstructionService.Worksite worksite,
             @NotNull PlacedStructure structure) {
        super(9, 6);
        this.menus = menus;
        this.viewer = viewer;
        this.key = worksite.getKey();
        this.structure = structure;
        this.job = structure.getJob();
        this.site = menus.getSettlers().site(key).orElseThrow();

        setItem(4, new SimpleItem(summary(menus, worksite, structure, Material.CLOCK).build()));

        setItem(9, label("clans.settler.crew.members"));
        final List<Settler> crew = menus.getRule().crew(key, job);
        for (int i = 0; i < crew.size() && i < 7; i++) {
            setItem(11 + i, new SimpleItem(builder(crew.get(i), crew).build()));
        }

        setItem(18, label("clans.settler.crew.free"));
        final boolean allowed = site.allows(viewer, key, SettlerAction.ASSIGN);
        final List<Settler> free = menus.getSettlers().roster(key).map(Roster::getSettlers).orElse(List.of()).stream()
                .filter(menus.getCrews()::isFree)
                .toList();
        if (free.isEmpty()) {
            setItem(FIRST_FREE_SLOT + 4, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.settler.crew.none_free").color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int i = 0; i < free.size() && i < FREE_SLOTS; i++) {
            final Settler candidate = free.get(i);
            final List<Settler> joined = new ArrayList<>(crew);
            joined.add(candidate);
            final ItemView.ItemViewBuilder view = builder(candidate, joined);
            if (allowed) {
                view.action(ClickActions.ALL, Translations.component("clans.settler.crew.add"));
            } else {
                view.lore(Translations.component("clans.settler.card.not_allowed").color(NamedTextColor.RED));
            }
            setItem(FIRST_FREE_SLOT + i, new SimpleItem(view.build(), click -> {
                if (allowed) {
                    menus.join(click.getPlayer(), structure.getId(), candidate.getId());
                }
            }));
        }

        setItem(45, new SimpleItem(ItemView.builder()
                .material(Material.ARROW)
                .displayName(Translations.component("clans.settler.crew.back").color(NamedTextColor.YELLOW))
                .action(ClickActions.ALL, Translations.component("clans.settler.crew.back"))
                .build(), click -> menus.openJobs(click.getPlayer())));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    /** A job at a glance: what it is, the Workforce it needs against what its crew brings, and its pace. */
    static @NotNull ItemView.ItemViewBuilder summary(@NotNull CrewMenus menus,
                                                     @NotNull ConstructionService.Worksite worksite,
                                                     @NotNull PlacedStructure structure, @NotNull Material icon) {
        final Job job = structure.getJob();
        final SiteKey key = worksite.getKey();
        final long now = menus.getConstruction().now();
        final int threshold = menus.getRule().threshold(structure, job);
        final int workforce = menus.getRule().workforce(key, structure, job);
        final double speed = menus.getRule().speed(key, structure, job);
        final Component name = menus.getCatalogue().find(structure.getType())
                .map(StructureType::getDisplayName)
                .orElse(Component.text(structure.getType()));

        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(icon)
                .displayName(name.color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(CrewJobsMenu.jobName(job).color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.settler.crew.workforce", Component.text(workforce),
                        Component.text(threshold)).color(workforce >= threshold ? NamedTextColor.GREEN : NamedTextColor.RED))
                .lore(Translations.component("clans.settler.crew.speed", Component.text(format(speed)))
                        .color(NamedTextColor.GRAY));
        if (job.isHeld()) {
            view.lore(Translations.component("clans.settler.crew.waiting",
                    Component.text(Math.max(0, threshold - workforce))).color(NamedTextColor.RED));
        } else {
            view.lore(Translations.component("clans.settler.crew.done_in",
                    Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(job.remainingMillis(now)))))
                    .color(NamedTextColor.GRAY));
        }
        return view;
    }

    /** A Builder and what it brings to this job beside {@code crew}. */
    private @NotNull ItemView.ItemViewBuilder builder(@NotNull Settler settler, @NotNull List<Settler> crew) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.PLAYER_HEAD)
                .displayName(Component.text(settler.getName(), settler.getRarity().getColor()))
                .frameLore(true)
                .lore(settler.getRarity().displayName());
        if (settler.getSpecialty() != null && settler.getProfession() != null) {
            menus.getProfessions().find(settler.getProfession()).ifPresent(profession -> view.lore(
                    Translations.component(profession.specialtyKey(settler.getSpecialty())).color(NamedTextColor.GRAY)));
        }
        final Optional<BuilderStats> stats = site.builderStats(key, settler, structure, job, crew);
        stats.ifPresent(found -> view.lore(Translations.component("clans.settler.crew.brings",
                Component.text(found.getWorkforce()), Component.text(format(found.getSpeed()))).color(NamedTextColor.GRAY)));
        return view;
    }

    private static @NotNull SimpleItem label(@NotNull String key) {
        return new SimpleItem(ItemView.builder()
                .material(Material.NAME_TAG)
                .displayName(Translations.component(key).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .build());
    }

    private static @NotNull String format(double speed) {
        return String.format(Locale.ROOT, "%.2f", speed);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.settler.crew.title");
    }
}
