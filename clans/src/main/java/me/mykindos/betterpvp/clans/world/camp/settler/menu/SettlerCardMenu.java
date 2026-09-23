package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.WorkplaceKind;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One settler: who it is, where it came from, what it does and how it feels, with buttons to put it to work or send
 * it away for players whose rank allows it. Everyone else can look.
 */
public class SettlerCardMenu extends AbstractGui implements Windowed {

    private final SettlerCards cards;
    private final Player viewer;
    private final SiteKey site;
    private final Settler settler;
    private final @Nullable Profession profession;

    SettlerCardMenu(@NotNull SettlerCards cards, @NotNull Player viewer, @NotNull SiteKey site,
                    @NotNull Settler settler) {
        super(9, 4);
        this.cards = cards;
        this.viewer = viewer;
        this.site = site;
        this.settler = settler;
        this.profession = settler.getProfession() == null ? null
                : cards.getProfessions().find(settler.getProfession()).orElse(null);

        setItem(4, identity());
        setItem(11, profession());
        setItem(13, morale());
        setItem(15, work());
        final List<Trait> traits = settler.getTraits().stream()
                .flatMap(id -> cards.getTraits().find(id).stream())
                .toList();
        final int first = 22 - traits.size() / 2;
        for (int i = 0; i < traits.size(); i++) {
            setItem(first + i, trait(traits.get(i)));
        }
        assignButton();
        dismissButton();
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull SimpleItem identity() {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.NAME_TAG)
                .displayName(Component.text(settler.getName(), settler.getRarity().getColor()))
                .frameLore(true)
                .lore(settler.getRarity().displayName());
        if (settler.getHistory() != null) {
            final ComponentLike[] args = settler.getHistoryArgs().stream()
                    .map(Component::text)
                    .toArray(ComponentLike[]::new);
            view.lore(Translations.component(settler.getHistory(), args).color(NamedTextColor.GRAY));
        }
        return new SimpleItem(view.build());
    }

    private @NotNull SimpleItem profession() {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(profession == null ? Material.LEATHER_BOOTS : Material.IRON_PICKAXE)
                .displayName((profession == null
                        ? Translations.component("clans.settler.card.no_profession")
                        : Translations.component(profession.getKey())).color(NamedTextColor.YELLOW))
                .frameLore(true);
        if (profession != null && settler.getSpecialty() != null) {
            view.lore(Translations.component(profession.specialtyKey(settler.getSpecialty())).color(NamedTextColor.GRAY));
        }
        if (profession == null) {
            view.lore(Translations.component("clans.settler.card.wanders").color(NamedTextColor.GRAY));
        }
        final double wage = cards.getPayroll().hourly(site, settler);
        if (wage > 0) {
            view.lore(Translations.component("clans.settler.card.wage",
                    Component.text(UtilFormat.formatNumber((int) Math.ceil(wage)), NamedTextColor.GOLD))
                    .color(NamedTextColor.GRAY));
        }
        return new SimpleItem(view.build());
    }

    private @NotNull SimpleItem morale() {
        final int morale = settler.getMorale();
        final NamedTextColor color = morale > 0 ? NamedTextColor.GREEN : morale < 0 ? NamedTextColor.RED : NamedTextColor.GRAY;
        return new SimpleItem(ItemView.builder()
                .material(Material.CAKE)
                .displayName(Translations.component("clans.settler.card.morale",
                        Component.text(morale, color)).color(NamedTextColor.YELLOW))
                .build());
    }

    private @NotNull SimpleItem work() {
        final String state = settler.getState().name().toLowerCase(Locale.ROOT);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(settler.getState() == SettlerState.WORKING ? Material.CLOCK : Material.COMPASS)
                .displayName(Translations.component("clans.settler.card.state." + state).color(NamedTextColor.YELLOW))
                .frameLore(true);
        if (settler.getAssignment() != null) {
            view.lore(Translations.component(settler.getAssignment().equals(CampGrounds.FARM)
                    ? "clans.settler.card.workplace.farm" : "clans.settler.card.workplace.construction")
                    .color(NamedTextColor.GRAY));
        }
        return new SimpleItem(view.build());
    }

    private @NotNull SimpleItem trait(@NotNull Trait trait) {
        final String group = trait.getGroup().name().toLowerCase(Locale.ROOT);
        return new SimpleItem(ItemView.builder()
                .material(trait.isTradeOff() ? Material.REDSTONE : Material.GLOWSTONE_DUST)
                .displayName(Translations.component(trait.nameKey())
                        .color(trait.isTradeOff() ? NamedTextColor.GOLD : NamedTextColor.AQUA))
                .frameLore(true)
                .lore(Translations.component(trait.descriptionKey()).color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.settler.card.group." + group).color(NamedTextColor.DARK_GRAY))
                .build());
    }

    /** Farmers are sent to and called back from the farm here. Builders open their crew, or the jobs they could join. */
    private void assignButton() {
        if (profession == null) {
            return;
        }
        if (profession.getWorkplaceKind() == WorkplaceKind.CONSTRUCTION) {
            setItem(29, new SimpleItem(ItemView.builder()
                    .material(Material.BRICKS)
                    .displayName(Translations.component("clans.settler.card.crews").color(NamedTextColor.GREEN))
                    .action(ClickActions.ALL, Translations.component("clans.settler.crew.open"))
                    .build(), click -> {
                        final UUID job = structureId(settler.getAssignment());
                        if (job == null) {
                            cards.getCrews().openJobs(click.getPlayer(), this);
                        } else {
                            cards.getCrews().openCrew(click.getPlayer(), job, this);
                        }
                    }));
            return;
        }

        final boolean working = settler.getAssignment() != null;
        final boolean allowed = cards.allows(viewer, site, SettlerAction.ASSIGN);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(working ? Material.OAK_DOOR : Material.WHEAT)
                .displayName(Translations.component(working ? "clans.settler.card.unassign" : "clans.settler.card.assign_farm")
                        .color(allowed ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        if (allowed) {
            view.action(ClickActions.ALL, Translations.component(working ? "clans.settler.card.unassign" : "clans.settler.card.assign_farm"));
        } else {
            view.frameLore(true).lore(Translations.component("clans.settler.card.not_allowed").color(NamedTextColor.RED));
        }
        setItem(29, new SimpleItem(view.build(), click -> {
            if (!allowed || !cards.allows(click.getPlayer(), site, SettlerAction.ASSIGN)) {
                return;
            }
            cards.after(click.getPlayer(), site, settler.getId(), working
                    ? cards.getService().unassign(site, settler.getId())
                    : cards.getService().assign(site, settler.getId(), profession.getWorkplace()));
        }));
    }

    private void dismissButton() {
        final boolean allowed = cards.allows(viewer, site, SettlerAction.DISMISS);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.BARRIER)
                .displayName(Translations.component("clans.settler.card.dismiss")
                        .color(allowed ? NamedTextColor.RED : NamedTextColor.GRAY))
                .frameLore(true)
                .lore(Translations.component("clans.settler.card.dismiss_warning").color(NamedTextColor.GRAY));
        if (allowed) {
            view.action(ClickActions.SHIFT, Translations.component("clans.settler.card.dismiss"));
        } else {
            view.lore(Translations.component("clans.settler.card.not_allowed").color(NamedTextColor.RED));
        }
        setItem(33, new SimpleItem(view.build(), click -> {
            if (!allowed || !click.getClickType().isShiftClick()
                    || !cards.allows(click.getPlayer(), site, SettlerAction.DISMISS)) {
                return;
            }
            final Player player = click.getPlayer();
            if (cards.getService().dismiss(site, settler.getId()).isSuccess()) {
                player.closeInventory();
                cards.tell(player, Translations.component("clans.settler.card.dismissed",
                        Component.text(settler.getName(), settler.getRarity().getColor())).color(NamedTextColor.GRAY));
            }
        }));
    }

    private static @Nullable UUID structureId(@Nullable String assignment) {
        if (assignment == null) {
            return null;
        }
        try {
            return UUID.fromString(assignment);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(settler.getName());
    }
}
