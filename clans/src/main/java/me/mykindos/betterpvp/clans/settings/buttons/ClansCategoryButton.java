package me.mykindos.betterpvp.clans.settings.buttons;

import me.mykindos.betterpvp.clans.settings.menus.ClansSettingsMenu;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ClansCategoryButton extends Button {

    private final GamerManager gamerManager;

    public ClansCategoryButton(GamerManager gamerManager) {
        super(1, new ItemStack(Material.DIAMOND_HELMET), ChatColor.GREEN.toString() + ChatColor.BOLD + "Clans Settings",
                ChatColor.GRAY + "View generic settings related to the clans gamemode");
        this.gamerManager = gamerManager;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        gamerOptional.ifPresent(gamer -> {

            ClansSettingsMenu clansSettingsMenu = new ClansSettingsMenu(player, gamer);
            MenuManager.openMenu(player, clansSettingsMenu);
            UtilSound.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1, false);
        });

    }
}
