package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
@ItemKey("core:hammer")
public class Hammer extends BaseItem implements NexoItem {
    private transient boolean registered;

    @Inject
    private Hammer() {
        super(translatableName("core.item.hammer.name"), ItemStack.of(Material.PAPER), ItemGroup.TOOL, ItemRarity.COMMON);

        addBaseComponent(DescriptionComponent.translatable(1, "core.item.hammer.lore"));
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_hammer_usable";
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, CutStone cutStone) {
        if (registered) return;
        registered = true;

        final String[] pattern = new String[] {
                "CCC",
                " L ",
                " S "
        };

        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        final BaseItem leather = itemFactory.getFallbackItem(Material.LEATHER);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('C', new RecipeIngredient(cutStone, 1));
        builder.setIngredient('L', new RecipeIngredient(leather, 1));
        builder.setIngredient('S', new RecipeIngredient(stick, 1));

        registry.registerRecipe(new NamespacedKey("core", "hammer"), builder.build());
    }
}
