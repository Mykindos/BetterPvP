package me.mykindos.betterpvp.progression.profession.woodcutting.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.impl.interaction.TreeFellerInteraction;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeUnlockService;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Singleton
@ItemKey("progression:willwood_axe")
public class WillwoodAxe extends BaseItem {

    private transient boolean registered;

    @Inject
    private WillwoodAxe(TreeFellerInteraction interaction) {
        super("Willwood Axe", Item.model(Material.IRON_AXE, "willwood_axe"), ItemGroup.TOOL, ItemRarity.RARE);
        addSerializableComponent(new DurabilityComponent(500));
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.BLOCK_BREAK, interaction)
                .build());
    }

    @Inject
    private void registerRecipe(RecipeUnlockService unlockService, CraftingRecipeRegistry registry, TreeBark treeBark, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem ironBlock = itemFactory.getFallbackItem(Material.IRON_BLOCK);
        final BaseItem goldBlock = itemFactory.getFallbackItem(Material.GOLD_BLOCK);
        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        String[] pattern = new String[] {
                "IGI",
                "ISI",
                "TST",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('I', new RecipeIngredient(ironBlock, 1));
        builder.setIngredient('G', new RecipeIngredient(goldBlock, 1));
        builder.setIngredient('S', new RecipeIngredient(stick, 1));
        builder.setIngredient('T', new RecipeIngredient(treeBark, 1));
        ShapedCraftingRecipe recipe = builder.build();

        // recreate it so we can add the unlock service
        final NamespacedKey key = new NamespacedKey("progression", "willwood_axe");
        recipe = new ShapedCraftingRecipe(recipe.getResultSupplier(), recipe.getIngredients(), recipe.getItemFactory(), recipe.needsBlueprint()) {
            @Override
            public boolean canCraft(@Nullable Player player) {
                return unlockService.isUnlocked(player, key);
            }
        };
        registry.registerRecipe(key, recipe);
    }


}
