package me.mykindos.betterpvp.core.settings.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class SettingsButton extends Button {

    @Getter
    protected final String setting;

    private final Gamer gamer;

    public SettingsButton(Gamer gamer, Enum<?> setting, int slot, ItemStack item, Component name, Component... lore) {
        super(slot, item, name, lore);
        this.gamer = gamer;
        this.setting = setting.name();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if(clickType == ClickType.LEFT) {
            this.gamer.saveProperty(setting, true, true);
        }else if(clickType == ClickType.RIGHT){
            this.gamer.saveProperty(setting, false, true);
        }

        UtilSound.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1, false);
    }

    @Override
    public double getClickCooldown() {
        return 1;
    }
}
