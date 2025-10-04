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
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
public class RangerBoots extends ArmorItem {

    private transient boolean registered;

    @Inject
    private RangerBoots(Champions champions) {
        super(champions, "Ranger Boots", ItemStack.of(Material.CHAINMAIL_BOOTS), ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem emerald = itemFactory.getFallbackItem(Material.EMERALD);
        String[] pattern = new String[] {
                "E E",
                "E E",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('E', new RecipeIngredient(emerald, 1));
        registry.registerRecipe(new NamespacedKey("champions", "ranger_boots"), builder.build());
    }
}
