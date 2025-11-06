package me.mykindos.betterpvp.core.block.impl.anvil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
public class AnvilItem extends BaseItem implements NexoItem {

    private static final ItemStack model;
    private transient boolean registered;

    static {
        model = ItemStack.of(Material.PAPER);
        model.editMeta(meta -> meta.setMaxStackSize(1));
    }

    @Inject
    private AnvilItem() {
        super("Anvil", model, ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_anvil";
    }

    @Override
    public boolean isFurniture() {
        return true;
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;

        final String[] pattern = new String[] {
                "BBB",
                " I ",
                "III"
        };

        final BaseItem ironIngot = itemFactory.getFallbackItem(Material.IRON_INGOT);
        final BaseItem ironBlock = itemFactory.getFallbackItem(Material.IRON_BLOCK);
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('I', new RecipeIngredient(ironIngot, 1));
        builder.setIngredient('B', new RecipeIngredient(ironBlock, 1));
        registry.registerRecipe(new NamespacedKey("core", "anvil"), builder.build());
    }
} 