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
@ItemKey("champions:ranger_helmet")
@FallbackItem(value = Material.CHAINMAIL_HELMET, keepRecipes = true)
public class RangerHelmet extends ArmorItem {

    private transient boolean registered;

    @Inject
    private RangerHelmet(Champions champions) {
        super(champions, "Ranger Helmet", ItemStack.of(Material.CHAINMAIL_HELMET), ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem emerald = itemFactory.getFallbackItem(Material.EMERALD);
        String[] pattern = new String[] {
                "EEE",
                "E E",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('E', new RecipeIngredient(emerald, 1));
        registry.registerRecipe(new NamespacedKey("champions", "ranger_helmet"), builder.build());
    }
}
