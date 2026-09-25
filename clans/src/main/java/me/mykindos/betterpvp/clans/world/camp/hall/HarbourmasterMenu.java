package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.settler.menu.SettlerItems;
import me.mykindos.betterpvp.clans.world.camp.upgrade.Harbourmaster;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Dock's Harbourmaster, reached from the hiring board: when the next boat is due, and once per cooldown either a
 * look at who is on it or a choice of the profession they all have.
 */
public class HarbourmasterMenu extends AbstractGui implements Windowed {

    private final HallMenus menus;
    private final SiteKey key;
    private final Windowed previous;

    HarbourmasterMenu(@NotNull HallMenus menus, @NotNull SiteKey key, @NotNull Windowed previous) {
        super(9, 4);
        this.menus = menus;
        this.key = key;
        this.previous = previous;
        final Harbourmaster harbourmaster = menus.getHarbourmaster();

        setItem(4, new SimpleItem(ItemView.builder()
                .material(Material.SPYGLASS)
                .displayName(Translations.component("clans.camp.upgrade.harbourmaster.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(status(menus, key))
                .build()));

        setItem(11, new SimpleItem(ItemView.builder()
                .material(Material.ENDER_EYE)
                .displayName(Translations.component("clans.camp.upgrade.harbourmaster.look.name")
                        .color(NamedTextColor.YELLOW))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.harbourmaster.look.description")
                        .color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.harbourmaster.use"))
                .build(), click -> {
                    final String problem = harbourmaster.look(click.getPlayer(), key);
                    if (problem != null) {
                        menus.tell(click.getPlayer(), problem);
                    }
                    reopen(click.getPlayer());
                }));

        setItem(15, new SimpleItem(ItemView.builder()
                .material(Material.COMPASS)
                .displayName(Translations.component("clans.camp.upgrade.harbourmaster.choose.name")
                        .color(NamedTextColor.YELLOW))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.harbourmaster.choose.description")
                        .color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.hall.open"))
                .build(), click -> new ChooseMenu().show(click.getPlayer())));

        final List<SettlerCandidate> foreseen = harbourmaster.foreseen(key);
        final int first = 22 - Math.min(foreseen.size(), 7) / 2;
        for (int i = 0; i < foreseen.size() && i < 7; i++) {
            setItem(first + i, new SimpleItem(candidate(foreseen.get(i))));
        }

        setItem(31, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    /** The hiring board's way in, showing when the next boat is due and whether today's use is spent. */
    static @NotNull SimpleItem button(@NotNull HallMenus menus, @NotNull SiteKey key, @NotNull Consumer<Player> open) {
        return new SimpleItem(ItemView.builder()
                .material(Material.SPYGLASS)
                .displayName(Translations.component("clans.camp.upgrade.harbourmaster.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(status(menus, key))
                .action(ClickActions.ALL, Translations.component("clans.camp.hall.open"))
                .build(), click -> open.accept(click.getPlayer()));
    }

    private static @NotNull List<Component> status(@NotNull HallMenus menus, @NotNull SiteKey key) {
        final Harbourmaster harbourmaster = menus.getHarbourmaster();
        final long now = System.currentTimeMillis();
        final List<Component> lines = new ArrayList<>();
        final long boat = harbourmaster.nextBoatAt(key);
        lines.add((boat == 0
                ? Translations.component("clans.camp.upgrade.harbourmaster.no_boat")
                : Translations.component("clans.camp.upgrade.harbourmaster.next_boat", time(boat - now)))
                .color(NamedTextColor.GRAY));
        final long ready = harbourmaster.readyAt(key);
        lines.add(now >= ready
                ? Translations.component("clans.camp.upgrade.harbourmaster.ready").color(NamedTextColor.GREEN)
                : Translations.component("clans.camp.upgrade.harbourmaster.used", time(ready - now))
                .color(NamedTextColor.RED));
        final String chosen = harbourmaster.chosen(key);
        if (chosen != null) {
            lines.add(Translations.component("clans.camp.upgrade.harbourmaster.chosen",
                    profession(menus, chosen)).color(NamedTextColor.AQUA));
        }
        return lines;
    }

    private static @NotNull Component time(long millis) {
        return Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(Math.max(0, millis))), NamedTextColor.WHITE);
    }

    private @NotNull ItemView candidate(@NotNull SettlerCandidate candidate) {
        final Settler settler = candidate.getSettler();
        final long price = menus.getRecruitment().price(key, candidate);
        return SettlerItems.identity(settler)
                .material(Material.PLAYER_HEAD)
                .lore(profession(menus, settler.getProfession()).color(NamedTextColor.YELLOW))
                .lore(price <= 0
                        ? Translations.component("clans.settler.recruit.free").color(NamedTextColor.GREEN)
                        : Translations.component("clans.settler.recruit.price",
                        Component.text(UtilFormat.formatNumber((int) price), NamedTextColor.GOLD))
                        .color(NamedTextColor.GRAY))
                .build();
    }

    private static @NotNull Component profession(@NotNull HallMenus menus, @Nullable String id) {
        return id == null
                ? Translations.component("clans.settler.card.no_profession")
                : menus.getProfessions().find(id)
                .map(found -> Translations.component(found.getKey()))
                .orElse(Component.text(id));
    }

    private void reopen(@NotNull Player player) {
        new HarbourmasterMenu(menus, key, previous).show(player);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.harbourmaster.name");
    }

    /** Every profession a boat can bring, one of which the next boat's settlers all take. */
    private final class ChooseMenu extends AbstractGui implements Windowed {

        private ChooseMenu() {
            super(9, 3);
            final List<String> choices = menus.getHarbourmaster().choices();
            final int first = 13 - Math.min(choices.size(), 7) / 2;
            for (int i = 0; i < choices.size() && i < 7; i++) {
                final String choice = choices.get(i);
                setItem(first + i, new SimpleItem(ItemView.builder()
                        .material(Material.IRON_PICKAXE)
                        .hideAdditionalTooltip(true)
                        .displayName(profession(menus, choice).color(NamedTextColor.YELLOW))
                        .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.harbourmaster.pick"))
                        .build(), click -> {
                            final String problem = menus.getHarbourmaster().choose(click.getPlayer(), key, choice);
                            if (problem != null) {
                                menus.tell(click.getPlayer(), problem);
                            } else {
                                menus.tell(click.getPlayer(), "clans.camp.upgrade.harbourmaster.picked",
                                        profession(menus, choice).color(NamedTextColor.YELLOW));
                            }
                            reopen(click.getPlayer());
                        }));
            }
            setItem(22, new BackButton(HarbourmasterMenu.this));
            setBackground(Menu.BACKGROUND_ITEM);
        }

        @Override
        public @NotNull Component getTitle() {
            return Translations.component("clans.camp.upgrade.harbourmaster.choose.name");
        }
    }
}
