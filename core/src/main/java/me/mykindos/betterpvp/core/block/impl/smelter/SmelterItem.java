package me.mykindos.betterpvp.core.block.impl.smelter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import me.mykindos.betterpvp.core.item.impl.CutStone;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
public class SmelterItem extends BaseItem implements NexoItem {

    private static final ItemStack model;
    private transient boolean registered;

    static {
        model = ItemStack.of(Material.PAPER);
        model.editMeta(meta -> meta.setMaxStackSize(1));
    }

    @Inject
    private SmelterItem() {
        super("Smelter", model, ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_furnace";
    }

    @Override
    public boolean isFurniture() {
        return true;
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, CutStone cutStone) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "CCC",
                "CFC",
                "COC"
        };
        final BaseItem blastFurnace = itemFactory.getFallbackItem(Material.BLAST_FURNACE);
        final BaseItem charcoal = itemFactory.getFallbackItem(Material.CHARCOAL);
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('C', new RecipeIngredient(cutStone, 1));
        builder.setIngredient('F', new RecipeIngredient(blastFurnace, 1));
        builder.setIngredient('O', new RecipeIngredient(charcoal, 1));
        registry.registerRecipe(new NamespacedKey("core", "smelter"), builder.build());
    }
}
