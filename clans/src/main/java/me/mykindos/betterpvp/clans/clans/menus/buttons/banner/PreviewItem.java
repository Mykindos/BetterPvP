package me.mykindos.betterpvp.clans.clans.menus.buttons.banner;

import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.impl.GuiSelectColor;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class PreviewItem extends ControlItem<BannerMenu> {

    @Override
    public ItemProvider getItemProvider(BannerMenu gui) {
        return ItemView.of(gui.getBuilder().build().get()).toBuilder()
                .displayName(Translations.component("clans.menu.banner.button.preview.name").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .hideAdditionalTooltip(true)
                .action(ClickActions.ALL, Translations.component("clans.menu.banner.button.preview.action"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        GuiSelectColor guiSelectColor = new GuiSelectColor(Translations.component("clans.menu.banner.select-color.title"), dyeColor -> {
            getGui().getBuilder().baseColor(BannerColor.fromDye(dyeColor));
            getGui().show(player);
        });
        guiSelectColor.show(player);
    }
}
