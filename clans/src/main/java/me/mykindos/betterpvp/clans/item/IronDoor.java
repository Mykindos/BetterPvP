package me.mykindos.betterpvp.clans.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapelessCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

@Singleton
@ItemKey("clans:iron_door")
@FallbackItem(value = Material.IRON_DOOR, keepRecipes = true)
public class IronDoor extends BaseItem {

    private static final List<Material> WOODEN_DOORS = List.of(
            Material.OAK_DOOR,
            Material.SPRUCE_DOOR,
            Material.BIRCH_DOOR,
            Material.JUNGLE_DOOR,
            Material.ACACIA_DOOR,
            Material.DARK_OAK_DOOR,
            Material.MANGROVE_DOOR,
            Material.CHERRY_DOOR,
            Material.BAMBOO_DOOR,
            Material.CRIMSON_DOOR,
            Material.WARPED_DOOR
    );

    private transient boolean registered;

    public IronDoor() {
        super("Iron Door", ItemStack.of(Material.IRON_DOOR), ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipes(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;

        for (Material woodenDoor : WOODEN_DOORS) {
            final BaseItem ingredient = itemFactory.getFallbackItem(woodenDoor);
            final ShapelessCraftingRecipe recipe = new ShapelessCraftingRecipe(
                    this,
                    Map.of(0, new RecipeIngredient(ingredient, 1)),
                    itemFactory,
                    false
            );
            final String doorName = woodenDoor.name().toLowerCase();
            registry.registerRecipe(new NamespacedKey("clans", "iron_door_from_" + doorName), recipe);
        }
    }
}
