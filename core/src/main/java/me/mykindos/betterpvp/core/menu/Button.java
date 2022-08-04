package me.mykindos.betterpvp.core.menu;

import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Data
public class Button {

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

    public Button(int slot, ItemStack item, String name, List<String> lore) {
        this.lore = lore.toArray(new String[0]);
        this.slot = slot;
        this.name = name;
        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(item, name, lore));
    }

    public void onClick(Player player, ClickType clickType) {

    }

    public double getClickCooldown(){
        return 0.05;
    }
}
