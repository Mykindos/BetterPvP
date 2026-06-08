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
import me.mykindos.betterpvp.progression.profession.fishing.bait.ability.LuckyBaitAbility;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Lucky Bait item that increases treasure chance.
 */
@Singleton
@ItemKey("progression:lucky_bait")
public class LuckyBaitItem extends BaitItem {

    private transient boolean registered;

    /**
     * Creates a new lucky bait item
     *
     * @param progression The progression plugin
     * @param ability The lucky bait ability
     */
    @Inject
    public LuckyBaitItem(Progression progression, LuckyBaitAbility ability) {
        super(progression, translatableName("progression.item.lucky-bait.name"), new ItemStack(Material.YELLOW_GLAZED_TERRACOTTA), ItemRarity.EPIC, ability);
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
                "ADA",
                "VAV",
        };
        BaseItem sunfishItem = itemFactory.getItemRegistry().getItem("progression:sunfish");
        BaseItem honeycomb = itemFactory.getFallbackItem(Material.HONEYCOMB);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('V', new RecipeIngredient(sunfishItem, 64));
        builder.setIngredient('A', new RecipeIngredient(honeycomb, 1));
        builder.setIngredient('D', new RecipeIngredient(magicEssence, 1));
        registry.registerRecipe(new NamespacedKey("progression", "lucky_bait"), builder.build());
    }
} 