package me.mykindos.betterpvp.core.menu;

import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Data
public class Button {

    private final int slot;
    private final ItemStack itemStack;
    private final Component name;
    private final Component[] lore;

    public Button(int slot, ItemStack item, String name, String... lore){
        this(slot, item, name, List.of(lore));
    }

    public Button(int slot, ItemStack item, String name, List<String> lore) {
        this(slot, item, Component.text(name), lore.stream().map(Component::text).toArray(Component[]::new));
    }

    public Button(int slot, ItemStack item, Component name, Component... lore) {
        this(slot, item, name, List.of(lore));
    }

    public Button(int slot, ItemStack item, Component name, List<Component> lore) {
        this.lore = lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toArray(Component[]::new);
        this.slot = slot;
        this.name = name.decoration(TextDecoration.ITALIC, false);
        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(item, this.name, List.of(this.lore)));
    }

    public void onClick(Player player, ClickType clickType) {

    }

    public double getClickCooldown(){
        return 0.05;
    }
}
