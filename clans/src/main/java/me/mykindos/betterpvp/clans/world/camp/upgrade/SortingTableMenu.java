package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/** The Sorting table's page: one button that files the member's main inventory into the camp's item chests. */
public class SortingTableMenu extends AbstractGui implements Windowed {

    SortingTableMenu(@NotNull SortingTable table, @NotNull SiteKey key, @NotNull Windowed previous) {
        super(9, 3);
        setItem(13, new SimpleItem(ItemView.builder()
                .material(Material.HOPPER)
                .displayName(Translations.component("clans.camp.upgrade.sorting_table.button")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.sorting_table.button.description")
                        .color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.sorting_table.use"))
                .build(), click -> table.sort(click.getPlayer(), key)));
        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.sorting_table.name");
    }
}
