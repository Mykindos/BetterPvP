package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class WarlockChestplate extends ArmorItem {
    @Inject
    private WarlockChestplate(Champions champions) {
        super(champions, "Warlock Chestplate", ItemStack.of(Material.NETHERITE_CHESTPLATE), ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        final BaseItem netherite = itemFactory.getFallbackItem(Material.NETHERITE_INGOT);
        String[] pattern = new String[] {
                "N N",
                "NNN",
                "NNN",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('N', new RecipeIngredient(netherite, 1));
        registry.registerRecipe(builder.build());
    }
}
