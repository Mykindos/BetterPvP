package me.mykindos.betterpvp.clans.settings.buttons;

import java.util.Optional;

import me.mykindos.betterpvp.clans.settings.menus.ClansSettingsMenu;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ClansCategoryButton extends Button {

    public ClansCategoryButton() {
        super(1,
                new ItemStack(Material.DIAMOND_HELMET),
                Component.text("Clans Settings", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text("View generic settings related to the clans gamemode", NamedTextColor.GRAY));
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {

        ClansSettingsMenu clansSettingsMenu = new ClansSettingsMenu(player, gamer);
        MenuManager.openMenu(player, clansSettingsMenu);
        UtilSound.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1, false);

    }
}
