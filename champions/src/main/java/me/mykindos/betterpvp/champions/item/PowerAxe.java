package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
public class PowerAxe extends WeaponItem {

    private transient boolean registered;

    @Inject
    private PowerAxe(Champions champions) {
        super(champions, "Power Axe", ItemStack.of(Material.DIAMOND_AXE), ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem diamondBlock = itemFactory.getFallbackItem(Material.DIAMOND_BLOCK);
        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        String[] pattern = new String[] {
                "DD",
                "SD",
                "S ",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('D', new RecipeIngredient(diamondBlock, 1));
        builder.setIngredient('S', new RecipeIngredient(stick, 1));
        registry.registerRecipe(new NamespacedKey("champions", "power_axe"), builder.build());
    }

}
