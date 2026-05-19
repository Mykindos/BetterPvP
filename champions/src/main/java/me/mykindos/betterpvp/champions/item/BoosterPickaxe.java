package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:booster_pickaxe")
@FallbackItem(value = Material.GOLDEN_PICKAXE, keepRecipes = false)
public class BoosterPickaxe extends BaseItem implements Reloadable {

    private static final int DURABILITY = 1000;

    private transient boolean registered;

    @Inject
    private BoosterPickaxe(Champions champions) {
        super("Booster Pickaxe", ItemStack.of(Material.GOLDEN_PICKAXE), ItemGroup.TOOL, ItemRarity.UNCOMMON);
        addSerializableComponent(new DurabilityComponent(DURABILITY));
        addSerializableComponent(new RuneContainerComponent());
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem goldBlock = itemFactory.getFallbackItem(Material.GOLD_BLOCK);
        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        String[] pattern = new String[] {
                "GGG",
                " S ",
                " S ",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('G', new RecipeIngredient(goldBlock, 1));
        builder.setIngredient('S', new RecipeIngredient(stick, 1));
        registry.registerRecipe(new NamespacedKey("core", "booster_pickaxe"), builder.build());
    }


    @Override
    public void reload() {
        final Config config = Config.item(Core.class, this);
        getComponent(DurabilityComponent.class).ifPresent(durability -> {
            durability.setMaxDamage(config.getConfig("durability", DURABILITY, Integer.class));
        });
    }

}
