package me.mykindos.betterpvp.core.settings.menus;

import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class GeneralSettingsMenu extends SettingSubMenu implements IRefreshingMenu {

    private final Gamer gamer;

    public GeneralSettingsMenu(Player player, Gamer gamer) {
        super(player, 9, Component.text("General Settings", NamedTextColor.BLACK).decorate(TextDecoration.BOLD));
        this.gamer = gamer;

        refresh();
    }

    @Override
    public void refresh() {

        Optional<Boolean> tipSettingOptional = gamer.getProperty(GamerProperty.TIPS_ENABLED);
        tipSettingOptional.ifPresent(tipSetting -> {
            addButton(new SettingsButton(gamer,
                    GamerProperty.TIPS_ENABLED,
                    0,
                    new ItemStack(Material.WRITABLE_BOOK),
                    Component.text("Tips", tipSetting ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.text("Whether to enable tips or not.", NamedTextColor.GRAY)));

        });

    }


}
