package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.component.armor.RoleArmorComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:ranger_chestplate")
@FallbackItem(value = Material.CHAINMAIL_CHESTPLATE, keepRecipes = true)
public class RangerChestplate extends ArmorItem {

    private transient boolean registered;

    @Inject
    private RangerChestplate(Champions champions) {
        super(champions, translatableName("champions.item.ranger-chestplate.name"), ItemStack.of(Material.CHAINMAIL_CHESTPLATE), ItemRarity.COMMON);
        addBaseComponent(new RoleArmorComponent(Role.RANGER));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem ingot = itemFactory.getFallbackItem(Material.EMERALD);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, new String[] {
                "I I",
                "III",
                "III",
        }, itemFactory);
        builder.setIngredient('I', new RecipeIngredient(ingot, 1));
        registry.registerRecipe(new NamespacedKey("champions", "ranger_chestplate"), builder.build());
    }
}
