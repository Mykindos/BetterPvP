package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:warlock_leggings")
@FallbackItem(value = Material.NETHERITE_LEGGINGS, keepRecipes = true)
public class WarlockLeggings extends ArmorItem {

    private transient boolean registered;

    @Inject
    private WarlockLeggings(Champions champions) {
        super(champions, "Warlock Leggings", ItemStack.of(Material.NETHERITE_LEGGINGS), ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem netherite = itemFactory.getFallbackItem(Material.NETHERITE_INGOT);
        String[] pattern = new String[] {
                "NNN",
                "N N",
                "N N",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('N', new RecipeIngredient(netherite, 1));
        registry.registerRecipe(new NamespacedKey("champions", "warlock_leggings"), builder.build());
    }
}
