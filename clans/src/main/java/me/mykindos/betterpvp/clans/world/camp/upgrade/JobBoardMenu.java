package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The Job board's page: every job in the camp, what it is, where it stands and who works it. */
public class JobBoardMenu extends AbstractGui implements Windowed {

    JobBoardMenu(@NotNull JobBoard board, @NotNull SiteKey camp, @Nullable Windowed previous) {
        super(9, 6);
        final List<JobBoard.Entry> entries = board.entries(camp).stream()
                .filter(entry -> type(board, entry.getStructure()).isPresent())
                .toList();
        if (entries.isEmpty()) {
            setItem(22, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.upgrade.job_board.none").color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int slot = 0; slot < entries.size() && slot < 45; slot++) {
            final JobBoard.Entry entry = entries.get(slot);
            final CampStructure type = type(board, entry.getStructure()).orElseThrow();
            setItem(slot, new SimpleItem(entry(board, camp, type, entry)
                    .lore(Component.empty())
                    .action(ClickActions.ALL, Translations.component("clans.camp.menu.structures.manage"))
                    .build(), click -> board.getStructureMenus().openActions(click.getPlayer(), camp,
                    entry.getStructure().getId(), new JobBoardMenu(board, camp, previous))));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull ItemView.ItemViewBuilder entry(@NotNull JobBoard board, @NotNull SiteKey camp,
                                                           @NotNull CampStructure type, @NotNull JobBoard.Entry entry) {
        final PlacedStructure structure = entry.getStructure();
        final Job job = entry.getJob();
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(type.getIcon())
                .displayName(type.getDisplayName().color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .glow(entry.getState() == JobBoard.State.READY)
                .frameLore(true)
                .lore(type.stageName(structure.getStage()).color(NamedTextColor.GRAY))
                .lore(kind(type, job).color(NamedTextColor.WHITE))
                .lore(state(board, camp, entry));

        view.lore(Component.empty());
        final List<Settler> crew = board.getCrews().crew(camp, job);
        if (crew.isEmpty()) {
            view.lore(Translations.component("clans.camp.upgrade.job_board.no_builders").color(NamedTextColor.GRAY));
        } else {
            view.lore(Translations.component("clans.camp.upgrade.job_board.builders").color(NamedTextColor.GRAY));
            crew.forEach(settler -> view.lore(Component.text(" " + settler.getName(), settler.getRarity().getColor())));
        }
        return view;
    }

    private static @NotNull Component kind(@NotNull CampStructure type, @NotNull Job job) {
        final String key = "clans.camp.upgrade.job_board.kind." + job.getKind().name().toLowerCase(Locale.ROOT);
        if (job.getKind() == JobKind.ADVANCE) {
            return Translations.component(key, type.stageName(job.getTargetStage()));
        }
        if (job.getKind() == JobKind.FIT_UPGRADE && job.getUpgrade() != null) {
            return Translations.component(key, Translations.component("clans.camp.upgrade." + job.getUpgrade() + ".name"));
        }
        return Translations.component(key);
    }

    private static @NotNull Component state(@NotNull JobBoard board, @NotNull SiteKey camp,
                                            @NotNull JobBoard.Entry entry) {
        final Job job = entry.getJob();
        return switch (entry.getState()) {
            case READY -> Translations.component("clans.camp.upgrade.job_board.ready").color(NamedTextColor.GOLD);
            case RUNNING -> entry.getRemainingMillis() == Long.MAX_VALUE
                    ? Translations.component("clans.camp.upgrade.job_board.stalled").color(NamedTextColor.RED)
                    : Translations.component("clans.camp.upgrade.job_board.running",
                    Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(Math.max(1000, entry.getRemainingMillis()))),
                            NamedTextColor.WHITE)).color(NamedTextColor.GREEN);
            case PAUSED -> {
                if (job.getHolds().contains(CrewRule.ID)) {
                    final CrewRule crews = board.getCrews();
                    final int missing = crews.threshold(entry.getStructure(), job)
                            - crews.workforce(camp, entry.getStructure(), job);
                    yield Translations.component("clans.camp.upgrade.job_board.paused_crew",
                            Component.text(Math.max(1, missing))).color(NamedTextColor.RED);
                }
                yield Translations.component("clans.camp.upgrade.job_board.paused").color(NamedTextColor.RED);
            }
        };
    }

    private static @NotNull Optional<CampStructure> type(@NotNull JobBoard board, @NotNull PlacedStructure structure) {
        return board.getCatalogue().find(structure.getType())
                .filter(CampStructure.class::isInstance)
                .map(CampStructure.class::cast);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.job_board.name");
    }
}
