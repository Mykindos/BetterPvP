package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.resource.ResourceKind;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Tally board's page: every resource chest in the camp with its share of each resource and of the capacity. */
public class TallyBoardMenu extends AbstractGui implements Windowed {

    TallyBoardMenu(@NotNull TallyBoard board, @NotNull Player viewer, @NotNull SiteKey key,
                   @Nullable Windowed previous) {
        super(9, 6);
        final List<TallyBoard.Share> tally = board.tally(viewer, key);

        setItem(4, new SimpleItem(ItemView.builder()
                .material(Material.OAK_SIGN)
                .displayName(Translations.component("clans.camp.upgrade.tally_board.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.tally_board.even", Component.text(tally.size()))
                        .color(NamedTextColor.GRAY))
                .build()));

        if (tally.isEmpty()) {
            setItem(22, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.upgrade.tally_board.none")
                            .color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int i = 0; i < tally.size() && i < 36; i++) {
            setItem(9 + i, new SimpleItem(chest(board.getChests(), tally.get(i))));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull ItemView chest(@NotNull StorehouseChests chests, @NotNull TallyBoard.Share share) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.CHEST)
                .displayName(chests.name(share.getChest()).color(NamedTextColor.YELLOW))
                .frameLore(true)
                .lore(StorehouseChests.position(share.getChest()).color(NamedTextColor.DARK_GRAY));
        for (ResourceKind kind : ResourceKind.values()) {
            view.lore(Translations.component("clans.camp.resource.amount",
                    Component.text(share.getAmounts().get(kind), NamedTextColor.WHITE), kind.displayName())
                    .color(NamedTextColor.GRAY));
        }
        view.lore(Translations.component("clans.camp.upgrade.tally_board.holds", Component.text(share.total()),
                Component.text(share.getCapacity())).color(NamedTextColor.GRAY));
        return view.build();
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.tally_board.name");
    }
}
