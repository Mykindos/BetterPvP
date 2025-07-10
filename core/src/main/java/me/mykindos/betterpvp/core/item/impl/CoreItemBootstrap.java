package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoFurniture;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
public class CoreItemBootstrap {

    private final Core core;
    private final ItemRegistry itemRegistry;

    @Inject
    private CoreItemBootstrap(Core core, ItemRegistry itemRegistry) {
        this.core = core;
        this.itemRegistry = itemRegistry;
    }

    @Inject
    private void registerRunes(UnbreakingRuneItem unbreakingRune,
                               ScorchingRuneItem scorchingRune) {
        itemRegistry.registerItem(new NamespacedKey(core, "unbreaking_rune"), unbreakingRune);
        itemRegistry.registerItem(new NamespacedKey(core, "scorching_rune"), scorchingRune);
    }

    @Inject
    private void registerItems(BlueprintItem blueprintItem, HammerItem hammerItem) {
        itemRegistry.registerItem(new NamespacedKey(core, "blueprint"), blueprintItem);
        itemRegistry.registerItem(new NamespacedKey(core, "hammer"), hammerItem);
    }

    @Inject
    private void registerMaterials(CoalItem coalItem, CharcoalItem charcoalItem) {
        itemRegistry.registerFallbackItem(new NamespacedKey(core, "coal"), Material.COAL, coalItem);
        itemRegistry.registerFallbackItem(new NamespacedKey(core, "charcoal"), Material.CHARCOAL, charcoalItem);
    }
}
