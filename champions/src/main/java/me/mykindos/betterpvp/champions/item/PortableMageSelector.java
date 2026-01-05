package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.item.ability.ArmorStorageEditAbility;
import me.mykindos.betterpvp.champions.item.ability.PortableClassAbility;
import me.mykindos.betterpvp.champions.item.component.storage.ArmorStorageComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.impl.Rope;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@ItemKey("champions:portable_mage_selector")
@Singleton
public class PortableMageSelector extends BaseItem {

    private transient boolean registered;

    @Inject
    public PortableMageSelector() {
        super("Portable Mage Selector",
                Item.builder(Material.YELLOW_HARNESS).maxStackSize(1).build(),
                ItemGroup.CONSUMABLE,
                ItemRarity.UNCOMMON);
        final PortableClassAbility portableClassAbility = new PortableClassAbility(Role.MAGE);
        portableClassAbility.setConsumesItem(true);
        addSerializableComponent(new ArmorStorageComponent(Role.MAGE, false));
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(portableClassAbility)
                .ability(new ArmorStorageEditAbility())
                .build());
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, Rope rope) {
        if (registered) return;
        registered = true;
        final BaseItem ore = itemFactory.getFallbackItem(Material.GOLD_INGOT);
        String[] pattern = new String[] {
                "IRI",
                "IRI",
                "IRI",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('R', new RecipeIngredient(rope, 1));
        builder.setIngredient('I', new RecipeIngredient(ore, 1));
        registry.registerRecipe(new NamespacedKey("champions", "portable_mage_selector"), builder.build());
    }
}
