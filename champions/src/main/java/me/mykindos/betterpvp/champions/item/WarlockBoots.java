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
@ItemKey("champions:warlock_boots")
@FallbackItem(value = Material.NETHERITE_BOOTS, keepRecipes = true)
public class WarlockBoots extends ArmorItem {

    private transient boolean registered;

    @Inject
    private WarlockBoots(Champions champions) {
        super(champions, translatableName("champions.item.warlock-boots.name"), ItemStack.of(Material.NETHERITE_BOOTS), ItemRarity.COMMON);
        addBaseComponent(new RoleArmorComponent(Role.WARLOCK));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem ingot = itemFactory.getFallbackItem(Material.NETHERITE_INGOT);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, new String[] {
                "I I",
                "I I",
        }, itemFactory);
        builder.setIngredient('I', new RecipeIngredient(ingot, 1));
        registry.registerRecipe(new NamespacedKey("champions", "warlock_boots"), builder.build());
    }
}
