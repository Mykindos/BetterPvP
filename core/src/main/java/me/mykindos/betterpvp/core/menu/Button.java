package me.mykindos.betterpvp.core.menu;

import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@Data
public abstract class Button {

    private final int slot;
    private final ItemStack itemStack;
    private final String name;
    private final String[] lore;

    public Button(int slot, ItemStack item, String name, String... lore){
        this.slot = slot;
        this.lore = lore;
        this.name = name;
        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(item, name, lore));
    }

    public abstract void onClick(Player player, ClickType clickType);

    public double getClickCooldown(){
        return 0.05;
    }
}
