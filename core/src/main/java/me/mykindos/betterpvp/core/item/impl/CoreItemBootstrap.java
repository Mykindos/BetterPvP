package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.ShapedRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
public class CoreItemBootstrap {

    private final Core core;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final RecipeRegistry recipeRegistry;

    @Inject
    private CoreItemBootstrap(Core core, ItemFactory itemFactory, ItemRegistry itemRegistry, RecipeRegistry recipeRegistry) {
        this.core = core;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        this.recipeRegistry = recipeRegistry;
    }

    @Inject
    private void registerRunes(UnbreakingRuneItem unbreakingRune,
                               ScorchingRuneItem scorchingRune) {
        itemRegistry.registerItem(new NamespacedKey(core, "unbreaking_rune"), unbreakingRune);
        itemRegistry.registerItem(new NamespacedKey(core, "scorching_rune"), scorchingRune);


        recipeRegistry.registerRecipe(new ShapedRecipe.Builder(unbreakingRune, new String[] {
                "S",
                "S"
        }, itemFactory).setIngredient('S', Material.STICK, 1).build());

        recipeRegistry.registerRecipe(new ShapedRecipe.Builder(unbreakingRune, new String[] {
                " E",
                "E",
                "E"
        }, itemFactory).setIngredient('E', Material.EMERALD, 2).needsBlueprint().build());

        recipeRegistry.registerRecipe(new ShapedRecipe.Builder(scorchingRune, new String[] {
                "E",
                "E"
        }, itemFactory).setIngredient('E', new RecipeIngredient(unbreakingRune, 1)).needsBlueprint().build());
    }

    @Inject
    private void registerItems(BlueprintItem blueprintItem) {
        itemRegistry.registerItem(new NamespacedKey(core, "blueprint"), blueprintItem);
    }

}
