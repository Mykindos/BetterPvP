package me.mykindos.betterpvp.core.recipe.crafting;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Called when a recipe is being prepared in a crafting interface.
 * This event is called before the result is displayed to the player.
 */
@Getter
public class PrepareCraftingRecipeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Map<Integer, ItemInstance> craftingMatrix;
    private final CraftingRecipe craftingRecipe;
    
    @Setter
    private boolean cancelled;
    
    /**
     * Creates a new PrepareCraftingRecipeEvent.
     * 
     * @param player The player viewing the crafting interface
     * @param craftingMatrix The items in the crafting matrix
     * @param craftingRecipe The recipe that was matched, or null if no match
     * @param result The result item that will be displayed
     */
    public PrepareCraftingRecipeEvent(@NotNull Player player, @NotNull Map<Integer, ItemInstance> craftingMatrix, @Nullable CraftingRecipe craftingRecipe) {
        super(player);
        this.craftingMatrix = craftingMatrix;
        this.craftingRecipe = craftingRecipe;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
} 