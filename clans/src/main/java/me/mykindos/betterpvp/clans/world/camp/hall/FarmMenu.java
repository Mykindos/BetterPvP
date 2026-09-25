package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.clans.world.camp.settler.FarmWorkplace;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The camp's farm: how much faster its crops grow and how often harvests drop more, the Farmers working it, and the
 * Farmers who could be sent to it.
 */
public class FarmMenu extends AbstractGui implements Windowed {

    FarmMenu(@NotNull HallMenus menus, @NotNull Player viewer, @NotNull SiteKey key, @Nullable Windowed previous) {
        super(9, 5);
        final FarmWorkplace farm = menus.getFarm();
        final List<Settler> working = farm.farmers(key);
        final int slots = farm.slots(key);

        final ItemView.ItemViewBuilder summary = ItemView.builder()
                .material(Material.HAY_BLOCK)
                .displayName(Translations.component("clans.camp.hall.farm.name").color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .lore(Translations.component("clans.camp.hall.farm.growth", percent(farm.growth(key)))
                        .color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.camp.hall.farm.extra_drop", percent(farm.extraDrop(key)))
                        .color(NamedTextColor.GRAY));
        if (slots >= 0) {
            summary.lore(Translations.component("clans.camp.hall.farm.slots", Component.text(working.size(), NamedTextColor.WHITE),
                    Component.text(slots, NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
        }
        setItem(4, new SimpleItem(summary.build()));

        for (int i = 0; i < working.size() && i < 7; i++) {
            final Settler farmer = working.get(i);
            setItem(10 + i, new SimpleItem(entry(farmer).action(ClickActions.ALL,
                    Translations.component("clans.camp.hall.settlers.open_card")).build(), click ->
                    menus.getCards().open(click.getPlayer(), key, farmer.getId(),
                            new FarmMenu(menus, click.getPlayer(), key, previous))));
        }

        final boolean allowed = menus.getPermissions().allows(viewer, key.getOwnerId(), SettlerAction.ASSIGN);
        final List<Settler> idle = farm.idleFarmers(key);
        if (idle.isEmpty()) {
            setItem(31, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.hall.farm.none_free").color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int i = 0; i < idle.size() && i < 7; i++) {
            final Settler farmer = idle.get(i);
            final ItemView.ItemViewBuilder view = entry(farmer);
            if (allowed) {
                view.action(ClickActions.ALL, Translations.component("clans.settler.card.assign_farm"));
            } else {
                view.lore(Translations.component("clans.settler.card.not_allowed").color(NamedTextColor.RED));
            }
            setItem(28 + i, new SimpleItem(view.build(), click -> {
                if (!allowed) {
                    return;
                }
                final SettlerResult result = menus.getSettlers().assign(key, farmer.getId(), CampGrounds.FARM);
                if (!result.isSuccess() && result.getReason() != null) {
                    menus.getCards().tell(click.getPlayer(), result.getReason());
                }
                new FarmMenu(menus, click.getPlayer(), key, previous).show(click.getPlayer());
            }));
        }
        setItem(40, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull ItemView.ItemViewBuilder entry(@NotNull Settler farmer) {
        return ItemView.builder()
                .material(Material.PLAYER_HEAD)
                .displayName(Component.text(farmer.getName(), farmer.getRarity().getColor()))
                .lore(farmer.getRarity().displayName())
                .lore(Translations.component("clans.settler.card.morale", Component.text(farmer.getMorale(), NamedTextColor.WHITE))
                        .color(NamedTextColor.GRAY));
    }

    private static @NotNull Component percent(double share) {
        return Component.text(Math.round(share * 100) + "%", NamedTextColor.GREEN);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.hall.farm.name");
    }
}
