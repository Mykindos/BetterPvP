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
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("clans:water_block")
@FallbackItem(Material.LAPIS_BLOCK)
public class WaterBlock extends BaseItem {

    private transient boolean registered;

    public WaterBlock() {
        super(translatableName("clans.item.water-block.name"), ItemStack.of(Material.LAPIS_BLOCK), ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem lapis = itemFactory.getFallbackItem(Material.LAPIS_LAZULI);
        String[] pattern = new String[] {
                "DDD",
                "DDD",
                "DDD",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('D', new RecipeIngredient(lapis, 1));
        registry.registerRecipe(new NamespacedKey("clans", "water_block"), builder.build());
    }
}
