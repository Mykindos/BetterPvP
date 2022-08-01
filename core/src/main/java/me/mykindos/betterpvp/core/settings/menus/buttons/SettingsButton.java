package me.mykindos.betterpvp.core.settings.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.core.menu.Button;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public abstract class SettingsButton extends Button {

    @Getter
    protected final String setting;

    public SettingsButton(String setting, boolean settingEnabled, int slot, ItemStack item, String name, String... lore) {
        super(slot, item, settingEnabled ? ChatColor.GREEN + name : ChatColor.RED + name, lore);
        this.setting = setting;
    }

    @Override
    public double getClickCooldown(){
        return 1;
    }
}
