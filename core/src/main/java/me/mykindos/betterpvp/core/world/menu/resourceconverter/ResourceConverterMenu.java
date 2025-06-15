package me.mykindos.betterpvp.core.world.menu.resourceconverter;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ResourceConverterMenu extends AbstractGui implements Windowed {

    private final ItemFactory itemFactory;
    
    public ResourceConverterMenu(Player player, ItemStack itemStack, ItemFactory itemFactory) {
        super(9, 1);
        this.itemFactory = itemFactory;

        setItem(1, new ResourceConverterButton(player, itemStack, getItemStack(Material.LEATHER)));
        setItem(2, new ResourceConverterButton(player, itemStack, getItemStack(Material.IRON_INGOT)));
        setItem(3, new ResourceConverterButton(player, itemStack, getItemStack(Material.GOLD_INGOT)));
        setItem(5, new ResourceConverterButton(player, itemStack, getItemStack(Material.DIAMOND)));
        setItem(6, new ResourceConverterButton(player, itemStack, getItemStack(Material.EMERALD)));
        setItem(7, new ResourceConverterButton(player, itemStack, getItemStack(Material.NETHERITE_INGOT)));
    }
    
    private ItemStack getItemStack(Material material) {
        return itemFactory.fromItemStack(new ItemStack(material)).orElseThrow().createItemStack();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Resource Converter");
    }

}
