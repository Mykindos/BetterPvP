package me.mykindos.betterpvp.clans.settings.buttons;

import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ClansSettingButton extends SettingsButton {

    private final Gamer gamer;

    public ClansSettingButton(Gamer gamer, Enum<?> setting, boolean settingEnabled, int slot, ItemStack item, String name, String... lore) {
        super(setting, settingEnabled, slot, item, name, lore);
        this.gamer = gamer;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        if(clickType == ClickType.LEFT) {
            gamer.saveProperty(setting, true);
        }else if(clickType == ClickType.RIGHT){
            gamer.saveProperty(setting, false);
        }

        UtilSound.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1, false);
    }
}
