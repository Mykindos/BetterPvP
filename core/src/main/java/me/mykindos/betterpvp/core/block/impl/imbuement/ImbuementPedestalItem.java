package me.mykindos.betterpvp.core.block.impl.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import me.mykindos.betterpvp.core.item.impl.CutStone;
import me.mykindos.betterpvp.core.item.impl.EternalFlame;
import me.mykindos.betterpvp.core.item.impl.RunicPlate;
import me.mykindos.betterpvp.core.item.impl.ShadowQuill;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
@ItemKey("core:imbuement_pedestal")
public class ImbuementPedestalItem extends BaseItem implements NexoItem {

    private static final ItemStack model;
    private transient boolean registered;

    static {
        model = ItemStack.of(Material.PAPER);
        model.editMeta(meta -> meta.setMaxStackSize(1));
    }

    @Inject
    private ImbuementPedestalItem() {
        super("Imbuement Pedestal", model, ItemGroup.BLOCK, ItemRarity.RARE);
    }

    @Override
    public @NotNull String getId() {
        return "imbuement_pedestal";
    }

    @Override
    public boolean isFurniture() {
        return true;
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, ShadowQuill shadowQuill,
                                CutStone cutStone, RunicPlate runicPlate, EternalFlame eternalFlame) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                " E ",
                "RSR",
                "CCC"
        };

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('E', new RecipeIngredient(eternalFlame, 1));
        builder.setIngredient('R', new RecipeIngredient(runicPlate, 1));
        builder.setIngredient('S', new RecipeIngredient(shadowQuill, 1));
        builder.setIngredient('C', new RecipeIngredient(cutStone, 1));
        registry.registerRecipe(new NamespacedKey("core", "imbuement_pedestal"), builder.build());
    }

} 