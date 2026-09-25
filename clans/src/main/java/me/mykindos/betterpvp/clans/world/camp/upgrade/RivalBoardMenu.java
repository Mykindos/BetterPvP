package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The rival board's page: the camps ranked around this one, in order, with this camp's own row marked. */
public class RivalBoardMenu extends AbstractGui implements Windowed {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    RivalBoardMenu(@NotNull RivalBoard board, @NotNull List<RivalBoard.Row> rows, @Nullable Windowed previous) {
        super(9, 4);
        for (int i = 0; i < rows.size() && i < SLOTS.length; i++) {
            setItem(SLOTS[i], new SimpleItem(row(board, rows.get(i))));
        }
        setItem(31, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull ItemView row(@NotNull RivalBoard board, @NotNull RivalBoard.Row row) {
        final Component name = board.clanName(row.getClanId())
                .<Component>map(Component::text)
                .orElseGet(() -> Translations.component("clans.camp.zone.unnamed"));
        final int change = row.getChange();
        final Component delta = Component.text((change > 0 ? "+" : "") + UtilFormat.formatNumber(change),
                change > 0 ? NamedTextColor.GREEN : change < 0 ? NamedTextColor.RED : NamedTextColor.WHITE);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(row.isOwn() ? Material.BELL : Material.WHITE_BANNER)
                .displayName(Translations.component("clans.camp.upgrade.rival_board.entry",
                                Component.text(row.getRank()), name)
                        .color(row.isOwn() ? NamedTextColor.GOLD : NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .glow(row.isOwn())
                .lore(Translations.component("clans.camp.prosperity.value",
                        Component.text(UtilFormat.formatNumber(row.getProsperity()), NamedTextColor.WHITE))
                        .color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.camp.upgrade.rival_board.change", delta).color(NamedTextColor.GRAY));
        if (row.isOwn()) {
            view.lore(Translations.component("clans.camp.upgrade.rival_board.own").color(NamedTextColor.AQUA));
        }
        return view.build();
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.rival_board.name");
    }
}
