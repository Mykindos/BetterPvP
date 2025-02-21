package me.mykindos.betterpvp.core.world.menu.resourceconverter;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ResourceConverterMenu extends AbstractGui implements Windowed {

    public ResourceConverterMenu(Player player, ItemStack itemStack, ItemHandler itemHandler) {
        super(9, 1);

        setItem(1, new ResourceConverterButton(player, itemStack, itemHandler.updateNames(new ItemStack(Material.LEATHER))));
        setItem(2, new ResourceConverterButton(player, itemStack, itemHandler.updateNames(new ItemStack(Material.IRON_INGOT))));
        setItem(3, new ResourceConverterButton(player, itemStack, itemHandler.updateNames(new ItemStack(Material.GOLD_INGOT))));
        setItem(5, new ResourceConverterButton(player, itemStack, itemHandler.updateNames(new ItemStack(Material.DIAMOND))));
        setItem(6, new ResourceConverterButton(player, itemStack, itemHandler.updateNames(new ItemStack(Material.EMERALD))));
        setItem(7, new ResourceConverterButton(player, itemStack, itemHandler.updateNames(new ItemStack(Material.NETHERITE_INGOT))));
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Resource Converter");
    }

}
