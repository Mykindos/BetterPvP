package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import me.mykindos.betterpvp.core.item.impl.Cloth;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
@ItemKey("core:workbench")
public class WorkbenchItem extends BaseItem implements NexoItem {

    private static final ItemStack model;
    private transient boolean registered;

    static {
        model = ItemStack.of(Material.PAPER);
        model.editMeta(meta -> meta.setMaxStackSize(1));
    }

    @Inject
    private WorkbenchItem() {
        super("Workbench", model, ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Override
    public @NotNull String getId() {
        return "workbench";
    }

    @Override
    public boolean isFurniture() {
        return true;
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, Cloth cloth) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "CCC",
                "STS",
                "S S"
        };
        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        final BaseItem craftingTable = itemFactory.getFallbackItem(Material.CRAFTING_TABLE);
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('C', new RecipeIngredient(cloth, 1));
        builder.setIngredient('S', new RecipeIngredient(stick, 1));
        builder.setIngredient('T', new RecipeIngredient(craftingTable, 1));
        registry.registerRecipe(new NamespacedKey("core", "workbench"), builder.build());
    }
}
