package me.mykindos.betterpvp.core.inventory.inventory;

import org.bukkit.inventory.ItemStack;

public interface StackSizeProvider {
    
    int getMaxStackSize(ItemStack itemStack);
    
}
