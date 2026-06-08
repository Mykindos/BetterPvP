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
@ItemKey("clans:iron_trapdoor")
@FallbackItem(value = Material.IRON_TRAPDOOR, keepRecipes = true)
public class IronTrapdoor extends BaseItem {

    private static final List<Material> WOODEN_TRAPDOORS = List.of(
            Material.OAK_TRAPDOOR,
            Material.SPRUCE_TRAPDOOR,
            Material.BIRCH_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR,
            Material.ACACIA_TRAPDOOR,
            Material.DARK_OAK_TRAPDOOR,
            Material.MANGROVE_TRAPDOOR,
            Material.CHERRY_TRAPDOOR,
            Material.BAMBOO_TRAPDOOR,
            Material.CRIMSON_TRAPDOOR,
            Material.WARPED_TRAPDOOR
    );

    private transient boolean registered;

    public IronTrapdoor() {
        super(translatableName("clans.item.iron-trapdoor.name"), ItemStack.of(Material.IRON_TRAPDOOR), ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipes(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;

        for (Material woodenTrapdoor : WOODEN_TRAPDOORS) {
            final BaseItem ingredient = itemFactory.getFallbackItem(woodenTrapdoor);
            final ShapelessCraftingRecipe recipe = new ShapelessCraftingRecipe(
                    this,
                    Map.of(0, new RecipeIngredient(ingredient, 1)),
                    itemFactory,
                    false
            );
            final String trapdoorName = woodenTrapdoor.name().toLowerCase();
            registry.registerRecipe(new NamespacedKey("clans", "iron_trapdoor_from_" + trapdoorName), recipe);
        }
    }
}

