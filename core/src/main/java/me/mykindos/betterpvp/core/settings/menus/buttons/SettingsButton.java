package me.mykindos.betterpvp.core.settings.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.core.menu.Button;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

public abstract class SettingsButton extends Button {

    @Getter
    protected final String setting;

    public SettingsButton(Enum<?> key, boolean settingEnabled, int slot, ItemStack item, String name, Component... lore) {
        this(key.name(), settingEnabled, slot, item, name, lore);
    }

    public SettingsButton(String setting, boolean settingEnabled, int slot, ItemStack item, String name, Component... lore) {
        super(slot, item, Component.text(name, settingEnabled ? NamedTextColor.GREEN : NamedTextColor.RED), lore);
        this.setting = setting;
    }

    @Override
    public double getClickCooldown() {
        return 1;
    }
}
