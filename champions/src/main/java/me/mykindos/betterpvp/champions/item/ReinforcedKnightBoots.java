package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.component.armor.RoleArmorComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

@Singleton
@ItemKey("champions:reinforced_knight_boots")
public class ReinforcedKnightBoots extends ArmorItem {

    private transient boolean registered;

    @Inject
    private ReinforcedKnightBoots(Champions champions) {
        super(champions, translatableName("champions.item.reinforced-knight-boots.name"), Item.builder(Material.IRON_BOOTS)
                .data(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(TrimMaterial.IRON, TrimPattern.HOST)).build())
                .build(), ItemRarity.UNCOMMON);
        addBaseComponent(new RoleArmorComponent(Role.KNIGHT));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem ingot = itemFactory.getFallbackItem(Material.IRON_INGOT);
        final BaseItem block = itemFactory.getFallbackItem(Material.IRON_BLOCK);

        final ShapedCraftingRecipe.Builder builder1 = new ShapedCraftingRecipe.Builder(this, new String[] {
                "B I",
                "I I",
        }, itemFactory);
        builder1.setIngredient('I', new RecipeIngredient(ingot, 1));
        builder1.setIngredient('B', new RecipeIngredient(block, 1));
        registry.registerRecipe(new NamespacedKey("champions", "reinforced_knight_boots_top"), builder1.build());

        final ShapedCraftingRecipe.Builder builder2 = new ShapedCraftingRecipe.Builder(this, new String[] {
                "I I",
                "B I",
        }, itemFactory);
        builder2.setIngredient('I', new RecipeIngredient(ingot, 1));
        builder2.setIngredient('B', new RecipeIngredient(block, 1));
        registry.registerRecipe(new NamespacedKey("champions", "reinforced_knight_boots_bottom"), builder2.build());
    }
}
