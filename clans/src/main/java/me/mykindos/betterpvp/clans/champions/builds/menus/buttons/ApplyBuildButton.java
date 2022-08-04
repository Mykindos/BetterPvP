package me.mykindos.betterpvp.clans.champions.builds.menus.buttons;

import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ApplyBuildButton extends Button {

    private final Gamer gamer;

    public ApplyBuildButton(Gamer gamer, int slot, ItemStack item, String name, String... lore) {
        super(slot, item, name, lore);
        this.gamer = gamer;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {

    }
}
