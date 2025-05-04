package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.world.events.PlayerUseStonecutterEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

@BPvPListener
@Singleton
public class SalvagerListener implements Listener {

    private final ItemHandler itemHandler;

    @Inject
    public SalvagerListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractStonecutter(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.STONECUTTER) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = hand.getItemMeta();
        if (itemMeta == null) return;
        if (!(itemMeta instanceof Damageable damageable)) return;
        if (!damageable.hasMaxDamage()) {
            UtilMessage.simpleMessage(player, "Salvage", "This item cannot be salvaged.");
            return;
        }

        PlayerUseStonecutterEvent useStonecutterEvent = UtilServer.callEvent(new PlayerUseStonecutterEvent(player, hand));
        if (useStonecutterEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Salvage", "This item cannot be salvaged.");
            return;
        }

        double remainingDamage = 1.0 - ((double) damageable.getDamage() / damageable.getMaxDamage());
        Map<ItemStack, Integer> materialCounts = null;

        // Get current recipe for item
        Recipe recipe = null;
        Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe curRecipe = recipeIterator.next();
            ItemStack result = curRecipe.getResult();

            // Check if the recipe result matches the target ItemStack
            if (result.getType() == hand.getType()) {
                recipe = curRecipe;
                break;
            }
        }

        switch (recipe) {
            case null -> {
                UtilMessage.simpleMessage(player, "Salvage", "This item cannot be salvaged.");
                return;
            }
            case ShapedRecipe shapedRecipe -> materialCounts = getMaterialCounts(shapedRecipe);
            case ShapelessRecipe shapelessRecipe -> materialCounts = getMaterialCounts(shapelessRecipe);
            default -> {
            }
        }

        if (materialCounts == null) {
            UtilMessage.simpleMessage(player, "Salvage", "This item cannot be salvaged.");
            return;
        }

        if (materialCounts.values().stream().anyMatch(count -> (int) Math.floor((count * remainingDamage) * 0.75) > 0)) {
            materialCounts.forEach((material, count) -> {
                int newCount = (int) Math.floor((count * remainingDamage) * 0.75);
                if (newCount == 0) return;
                ItemStack newMaterial = material.clone();
                newMaterial.setAmount(newCount);
                itemHandler.updateNames(newMaterial);
                UtilItem.insert(player, newMaterial);
            });

            player.getInventory().setItemInMainHand(null);
            UtilSound.playSound(player.getWorld(), event.getClickedBlock().getLocation(), Sound.UI_STONECUTTER_TAKE_RESULT, 0.4f, 1.5f);
            UtilMessage.simpleMessage(player, "Salvage", "Item salvaged.");
        } else {
            UtilMessage.simpleMessage(player, "Salvage", "This item is too damaged to salvage.");
        }
    }

    @SuppressWarnings("deprecation")
    private Map<ItemStack, Integer> getMaterialCounts(ShapedRecipe recipe) {
        Map<ItemStack, Integer> materialCounts = new HashMap<>();
        Map<Character, ItemStack> ingredientMap = recipe.getIngredientMap();
        String[] shape = recipe.getShape();

        // Iterate through each row of the shape
        for (String row : shape) {
            // Iterate through each character in the row
            for (char key : row.toCharArray()) {
                ItemStack ingredient = ingredientMap.get(key);
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    // Clone the ingredient to avoid modifying the original
                    ItemStack ingredientClone = ingredient.clone();
                    // Use amount of 1 as key, then multiply by count later
                    ingredientClone.setAmount(1);

                    // Add or update the count in the map
                    materialCounts.merge(ingredientClone, ingredient.getAmount(), Integer::sum);
                }
            }
        }

        return materialCounts;
    }

    private Map<ItemStack, Integer> getMaterialCounts(ShapelessRecipe recipe) {
        Map<ItemStack, Integer> materialCounts = new HashMap<>();
        for (ItemStack ingredient : recipe.getIngredientList()) {
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                ItemStack ingredientClone = ingredient.clone();
                ingredientClone.setAmount(1); // Normalize to 1 for key
                materialCounts.merge(ingredientClone, ingredient.getAmount(), Integer::sum);
            }
        }
        return materialCounts;
    }

    @EventHandler (ignoreCancelled = true)
    public void onMoveItemToSalvager(InventoryMoveItemEvent event) {
        Location location = event.getDestination().getLocation();

        if (location != null) {
            if (location.getBlock().getType() == Material.STONECUTTER) {
                event.setCancelled(true);
            }
        }
    }
}
