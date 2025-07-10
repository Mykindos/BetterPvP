package me.mykindos.betterpvp.core.recipe.crafting;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Called when a player crafts an item using a custom recipe.
 * This event is called before the items are consumed and the result is given.
 */
@Getter
public class CraftingRecipeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Map<Integer, ItemInstance> craftingMatrix;
    private final CraftingRecipe craftingRecipe;
    
    @Setter
    private boolean cancelled;
    
    @Setter
    private ItemInstance result;
    
    /**
     * Creates a new CraftingRecipeEvent.
     * 
     * @param player The player crafting the item
     * @param craftingMatrix The items in the crafting matrix
     * @param craftingRecipe The recipe being crafted
     * @param result The result item that will be given to the player
     */
    public CraftingRecipeEvent(@NotNull Player player, @NotNull Map<Integer, ItemInstance> craftingMatrix,
                               @NotNull CraftingRecipe craftingRecipe, @NotNull ItemInstance result) {
        super(player);
        this.craftingMatrix = craftingMatrix;
        this.craftingRecipe = craftingRecipe;
        this.result = result;
    }
    
    /**
     * Gets the result as an ItemStack for use with Bukkit APIs.
     * @return The result as an ItemStack
     */
    public ItemStack getResultAsItemStack() {
        return result.createItemStack();
    }
    
    /**
     * Sets the result from an ItemStack.
     * @param itemStack The ItemStack to set as the result
     * @param itemFactory The ItemFactory to use for conversion
     */
    public void setResultFromItemStack(ItemStack itemStack, ItemFactory itemFactory) {
        if (itemStack == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        
        itemFactory.fromItemStack(itemStack).ifPresent(instance -> this.result = instance);
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
} 