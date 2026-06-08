package me.mykindos.betterpvp.progression.profession.fishing.bait;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.access.RestrictedAccessComponent;
import me.mykindos.betterpvp.core.item.impl.MagicEssence;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.bait.ability.SpeedyBaitAbility;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Speedy Bait item that increases fishing speed.
 */
@Singleton
@ItemKey("progression:speedy_bait")
public class SpeedyBaitItem extends BaitItem {

    private transient boolean registered;

    /**
     * Creates a new speedy bait item
     *
     * @param ability The speedy bait ability
     * @param loreRenderer The lore renderer
     */
    @Inject
    public SpeedyBaitItem(Progression progression, SpeedyBaitAbility ability) {
        super(progression, translatableName("progression.item.speedy-bait.name"), new ItemStack(Material.ORANGE_GLAZED_TERRACOTTA), ItemRarity.UNCOMMON, ability);
        addBaseComponent(new RestrictedAccessComponent(Set.of(AccessScope.CRAFT)));
    }


    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                MagicEssence magicEssence) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "VAV",
                "AVA",
                "VAV",
        };
        BaseItem tuna = itemFactory.getItemRegistry().getItem("progression:tuna");
        BaseItem honeyBlock = itemFactory.getFallbackItem(Material.HONEYCOMB);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('V', new RecipeIngredient(tuna, 16));
        builder.setIngredient('A', new RecipeIngredient(honeyBlock, 1));
        registry.registerRecipe(new NamespacedKey("progression", "speedy_bait"), builder.build());
    }
} 