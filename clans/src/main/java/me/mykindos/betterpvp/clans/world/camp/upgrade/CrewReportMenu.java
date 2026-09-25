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
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.crew.CrewTally;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The Crew report's page: the camp's running jobs and the last finished job on each structure. */
public class CrewReportMenu extends AbstractGui implements Windowed {

    private final CrewReport report;
    private final SiteKey camp;

    CrewReportMenu(@NotNull CrewReport report, @NotNull SiteKey camp, @Nullable Windowed previous) {
        super(9, 6);
        this.report = report;
        this.camp = camp;
        final List<CrewTally> tallies = report.getTallies().tallies(camp).stream()
                .filter(tally -> type(tally).isPresent())
                .toList();
        if (tallies.isEmpty()) {
            setItem(22, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.upgrade.crew_report.none").color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int slot = 0; slot < tallies.size() && slot < 45; slot++) {
            final CrewTally tally = tallies.get(slot);
            setItem(slot, new SimpleItem(summary(tally)
                    .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.crew_report.open"))
                    .build(), click -> new JobMenu(tally).show(click.getPlayer())));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    /** A job at a glance: its structure, what it was, whether it is still running and how many worked it. */
    private @NotNull ItemView.ItemViewBuilder summary(@NotNull CrewTally tally) {
        final CampStructure type = type(tally).orElseThrow();
        return ItemView.builder()
                .material(type.getIcon())
                .displayName(type.getDisplayName().color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .glow(!tally.isFinished())
                .lore(kind(type, tally).color(NamedTextColor.WHITE))
                .lore(tally.isFinished()
                        ? Translations.component("clans.camp.upgrade.crew_report.finished").color(NamedTextColor.GRAY)
                        : Translations.component("clans.camp.upgrade.crew_report.running").color(NamedTextColor.GREEN))
                .lore(Translations.component("clans.camp.upgrade.crew_report.builders",
                        Component.text(tally.getShares().size(), NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
    }

    private static @NotNull Component kind(@NotNull CampStructure type, @NotNull CrewTally tally) {
        final String key = "clans.camp.upgrade.job_board.kind." + tally.getKind().name().toLowerCase(Locale.ROOT);
        if (tally.getKind() == JobKind.ADVANCE) {
            return Translations.component(key, type.stageName(tally.getTargetStage()));
        }
        if (tally.getKind() == JobKind.FIT_UPGRADE && tally.getUpgrade() != null) {
            return Translations.component(key, Translations.component("clans.camp.upgrade." + tally.getUpgrade() + ".name"));
        }
        return Translations.component(key);
    }

    private @NotNull Optional<CampStructure> type(@NotNull CrewTally tally) {
        return report.getCatalogue().find(tally.getStructureType())
                .filter(CampStructure.class::isInstance)
                .map(CampStructure.class::cast);
    }

    private static @NotNull String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.crew_report.name");
    }

    /** One job's Builders: each one's share of the work, the Speed the job could not use and how long it worked. */
    private final class JobMenu extends AbstractGui implements Windowed {

        private JobMenu(@NotNull CrewTally tally) {
            super(9, 6);
            setItem(4, new SimpleItem(summary(tally)
                    .lore(Component.empty())
                    .lore(Translations.component("clans.camp.upgrade.crew_report.explain").color(NamedTextColor.GRAY))
                    .build()));
            final List<Map.Entry<UUID, CrewTally.Share>> shares = List.copyOf(tally.getShares().entrySet());
            if (shares.isEmpty()) {
                setItem(22, new SimpleItem(ItemView.builder()
                        .material(Material.BARRIER)
                        .displayName(Translations.component("clans.camp.upgrade.crew_report.no_builders")
                                .color(NamedTextColor.GRAY))
                        .build()));
            }
            for (int i = 0; i < shares.size() && i < 27; i++) {
                final UUID id = shares.get(i).getKey();
                final CrewTally.Share share = shares.get(i).getValue();
                final Optional<Settler> settler = report.getSettlers().roster(camp).flatMap(roster -> roster.find(id));
                setItem(18 + i, new SimpleItem(ItemView.builder()
                        .material(Material.PLAYER_HEAD)
                        .displayName(settler.<Component>map(found -> Component.text(found.getName(),
                                        found.getRarity().getColor()))
                                .orElse(Translations.component("clans.camp.upgrade.crew_report.gone")
                                        .color(NamedTextColor.GRAY)))
                        .lore(Translations.component("clans.camp.upgrade.crew_report.share",
                                Component.text(Math.round(tally.shareOf(id) * 100) + "%", NamedTextColor.WHITE))
                                .color(NamedTextColor.GRAY))
                        .lore(Translations.component("clans.camp.upgrade.crew_report.wasted",
                                Component.text(format(share.wastedSpeed()) + "x",
                                        share.wastedSpeed() > 0 ? NamedTextColor.RED : NamedTextColor.GREEN))
                                .color(NamedTextColor.GRAY))
                        .lore(Translations.component("clans.camp.upgrade.crew_report.worked",
                                Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(share.getMillis())),
                                        NamedTextColor.WHITE)).color(NamedTextColor.GRAY))
                        .build()));
            }
            setItem(49, new BackButton(CrewReportMenu.this));
            setBackground(Menu.BACKGROUND_ITEM);
        }

        @Override
        public @NotNull Component getTitle() {
            return Translations.component("clans.camp.upgrade.crew_report.job_title");
        }
    }
}
