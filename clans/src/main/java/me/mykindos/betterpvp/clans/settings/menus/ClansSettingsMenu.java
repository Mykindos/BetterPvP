package me.mykindos.betterpvp.clans.settings.menus;

import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.settings.menus.SettingSubMenu;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ClansSettingsMenu extends SettingSubMenu implements IRefreshingMenu {

    private final Gamer gamer;

    public ClansSettingsMenu(Player player, Gamer gamer) {
        super(player, 9, Component.text("Clans Settings", NamedTextColor.BLACK).decorate(TextDecoration.BOLD));
        this.gamer = gamer;

        refresh();
    }

    @Override
    public void refresh() {

        Optional<Boolean> sidebarSettingOptional = gamer.getProperty(GamerProperty.SIDEBAR_ENABLED);
        sidebarSettingOptional.ifPresent(sidebarSetting -> {
            addButton(new SettingsButton(gamer,
                    GamerProperty.SIDEBAR_ENABLED,
                    0,
                    new ItemStack(Material.IRON_BARS),
                    Component.text("Sidebar", sidebarSetting ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.text("Whether to display the sidebar or not", NamedTextColor.GRAY)));

        });

    }


}
