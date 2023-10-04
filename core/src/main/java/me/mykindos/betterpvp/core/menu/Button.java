package me.mykindos.betterpvp.core.menu;

import lombok.Data;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class Button {

    private final int slot;
    protected Component name;
    protected List<Component> lore;
    protected ItemStack itemStack;

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
        this.lore = lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList());
        this.slot = slot;
        this.name = name.decoration(TextDecoration.ITALIC, false);
        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(item, this.name, this.lore));
    }

    public Button(int slot, ItemStack item) {
        this.slot= slot;
        this.lore = item.getItemMeta().lore();
        this.name = item.displayName().decoration(TextDecoration.ITALIC, false);
        this.itemStack = item;
    }

    public void onClick(Player player, Gamer gamer, ClickType clickType) {

    }

    public double getClickCooldown(){
        return 0.05;
    }
}
