package me.mykindos.betterpvp.clans.settings.menus;

import me.mykindos.betterpvp.clans.settings.buttons.ClansSettingButton;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.settings.menus.SettingSubMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ClansSettingsMenu extends SettingSubMenu implements IRefreshingMenu {

    private final Gamer gamer;

    public ClansSettingsMenu(Player player, Gamer gamer) {
        super(player, 9, ChatColor.GREEN.toString() + ChatColor.BOLD + "Clans Settings");
        this.gamer = gamer;

        refresh();
    }

    @Override
    public void refresh() {

        Optional<Boolean> sidebarSettingOptional = gamer.getProperty(GamerProperty.SIDEBAR_ENABLED);
        sidebarSettingOptional.ifPresent(sidebarSetting -> {
            addButton(new ClansSettingButton(gamer, GamerProperty.SIDEBAR_ENABLED, sidebarSetting,
                    0, new ItemStack(Material.IRON_BARS), "Sidebar", ChatColor.GRAY + "Whether to display the sidebar or not"));
        });

    }


}
