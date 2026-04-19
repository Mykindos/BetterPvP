package me.mykindos.betterpvp.progression.profession.woodcutting.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeUnlockService;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Singleton
@ItemKey("progression:willwood_axe")
@FallbackItem(value = Material.STONE_AXE, keepRecipes = true)
public class WillwoodAxe extends BaseItem {

    private transient boolean registered;

    @Inject
    private WillwoodAxe() {
        super("Willwood Axe", Item.model(Material.IRON_AXE, "willwood_axe"), ItemGroup.TOOL, ItemRarity.RARE);
    }

    @Inject
    private void registerRecipe(RecipeUnlockService unlockService, CraftingRecipeRegistry registry, TreeBark treeBark, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem ironBlock = itemFactory.getFallbackItem(Material.IRON_BLOCK);
        final BaseItem goldBlock = itemFactory.getFallbackItem(Material.GOLD_BLOCK);
        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        String[] pattern = new String[] {
                "IGI",
                "ISI",
                "TST",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('I', new RecipeIngredient(ironBlock, 1));
        builder.setIngredient('G', new RecipeIngredient(goldBlock, 1));
        builder.setIngredient('S', new RecipeIngredient(stick, 1));
        builder.setIngredient('T', new RecipeIngredient(treeBark, 1));
        final ShapedCraftingRecipe recipe = builder.build();

        // recreate it so we can add the unlock service
        final NamespacedKey key = new NamespacedKey("progression", "willwood_axe");
        new ShapedCraftingRecipe(recipe.getResultSupplier(), recipe.getIngredients(), recipe.getItemFactory(), recipe.needsBlueprint()) {
            @Override
            public boolean canCraft(@Nullable Player player) {
                return unlockService.isUnlocked(player, key);
            }
        };
        registry.registerRecipe(key, recipe);
    }


}
