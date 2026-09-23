package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Everyone living in the camp, with how many more it has room for and how many of each profession can work. Filters
 * narrow it by profession and by what they are doing, and clicking a settler opens its card.
 */
public class SettlerRosterMenu extends AbstractGui implements Windowed {

    private static final int FIRST_SLOT = 9;
    private static final int SLOTS = 36;
    /** Profession filters in the order a click cycles through them. Null shows everyone, "none" those without one. */
    private static final List<String> PROFESSIONS = Arrays.asList(null, CampProfessions.BUILDER,
            CampProfessions.FARMER, "none");
    private static final List<SettlerState> STATES = Arrays.asList(null, SettlerState.WORKING, SettlerState.IDLE,
            SettlerState.STRIKING);

    private final HallMenus menus;
    private final SiteKey key;
    private final @Nullable Windowed previous;
    private final int profession;
    private final int state;

    SettlerRosterMenu(@NotNull HallMenus menus, @NotNull SiteKey key, @Nullable Windowed previous, int profession,
                      int state) {
        super(9, 6);
        this.menus = menus;
        this.key = key;
        this.previous = previous;
        this.profession = profession;
        this.state = state;

        final Roster roster = menus.getSettlers().roster(key).orElseGet(Roster::new);
        setItem(4, summary(roster));
        setItem(1, professionFilter());
        setItem(7, stateFilter());

        final List<Settler> shown = roster.getSettlers().stream().filter(this::shows).toList();
        for (int i = 0; i < shown.size() && i < SLOTS; i++) {
            final Settler settler = shown.get(i);
            setItem(FIRST_SLOT + i, new SimpleItem(entry(settler), click ->
                    menus.getCards().open(click.getPlayer(), key, settler.getId(), this)));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private boolean shows(@NotNull Settler settler) {
        final String wanted = PROFESSIONS.get(profession);
        final SettlerState wantedState = STATES.get(state);
        final boolean professionMatches = wanted == null
                || ("none".equals(wanted) ? settler.getProfession() == null : settler.hasProfession(wanted));
        return professionMatches && (wantedState == null || settler.getState() == wantedState);
    }

    private @NotNull SimpleItem summary(@NotNull Roster roster) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.BELL)
                .displayName(Translations.component("clans.camp.hall.settlers.population",
                        Component.text(roster.size()), Component.text(menus.getSettlers().populationCap(key)))
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true);
        for (Profession each : menus.getProfessions().all()) {
            final OptionalInt cap = menus.getSettlers().workingCap(key, each.getId());
            if (cap.isPresent()) {
                view.lore(Translations.component("clans.camp.hall.settlers.working",
                        Translations.component(each.getKey()), Component.text(roster.working(each.getId())),
                        Component.text(cap.getAsInt())).color(NamedTextColor.GRAY));
            }
        }
        view.lore(Translations.component("clans.camp.prosperity.value",
                Component.text(menus.getProsperity().of(key), NamedTextColor.GOLD)).color(NamedTextColor.GRAY));
        if (roster.size() > 0) {
            final double morale = roster.getSettlers().stream().mapToInt(Settler::getMorale).average().orElse(0);
            view.lore(Translations.component("clans.camp.hall.settlers.morale", Component.text((int) Math.round(morale)))
                    .color(NamedTextColor.GRAY));
        }
        return new SimpleItem(view.build());
    }

    private @NotNull SimpleItem professionFilter() {
        final String wanted = PROFESSIONS.get(profession);
        final Component shown = wanted == null ? Translations.component("clans.camp.hall.settlers.everyone")
                : "none".equals(wanted) ? Translations.component("clans.settler.card.no_profession")
                : menus.getProfessions().find(wanted).map(found -> Translations.component(found.getKey()))
                .orElse(Component.text(wanted));
        return filter(Material.IRON_PICKAXE, "clans.camp.hall.settlers.filter_profession", shown,
                player -> new SettlerRosterMenu(menus, key, previous, (profession + 1) % PROFESSIONS.size(), state)
                        .show(player));
    }

    private @NotNull SimpleItem stateFilter() {
        final SettlerState wanted = STATES.get(state);
        final Component shown = wanted == null ? Translations.component("clans.camp.hall.settlers.everyone")
                : Translations.component("clans.settler.card.state." + wanted.name().toLowerCase(Locale.ROOT));
        return filter(Material.CLOCK, "clans.camp.hall.settlers.filter_state", shown,
                player -> new SettlerRosterMenu(menus, key, previous, profession, (state + 1) % STATES.size())
                        .show(player));
    }

    private static @NotNull SimpleItem filter(@NotNull Material icon, @NotNull String name, @NotNull Component shown,
                                              @NotNull Consumer<Player> next) {
        return new SimpleItem(ItemView.builder()
                .material(icon)
                .displayName(Translations.component(name, shown.color(NamedTextColor.WHITE)).color(NamedTextColor.YELLOW))
                .action(ClickActions.ALL, Translations.component("clans.camp.hall.settlers.next_filter"))
                .build(), click -> next.accept(click.getPlayer()));
    }

    private @NotNull ItemView entry(@NotNull Settler settler) {
        final Component work = settler.getProfession() == null
                ? Translations.component("clans.settler.card.no_profession")
                : menus.getProfessions().find(settler.getProfession())
                .map(found -> Translations.component(found.getKey()))
                .orElse(Component.text(settler.getProfession()));
        final int morale = settler.getMorale();
        return ItemView.builder()
                .material(Material.PLAYER_HEAD)
                .displayName(Component.text(settler.getName(), settler.getRarity().getColor()))
                .frameLore(true)
                .lore(settler.getRarity().displayName())
                .lore(work.color(NamedTextColor.YELLOW))
                .lore(Translations.component("clans.settler.card.state." + settler.getState().name().toLowerCase(Locale.ROOT))
                        .color(settler.getState() == SettlerState.STRIKING ? NamedTextColor.RED : NamedTextColor.GRAY))
                .lore(Translations.component("clans.settler.card.morale", Component.text(morale,
                        morale > 0 ? NamedTextColor.GREEN : morale < 0 ? NamedTextColor.RED : NamedTextColor.GRAY))
                        .color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.hall.settlers.open_card"))
                .build();
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.hall.settlers.name");
    }
}
