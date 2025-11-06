package me.mykindos.betterpvp.core.block.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.impl.anvil.Anvil;
import me.mykindos.betterpvp.core.block.impl.anvil.AnvilItem;
import me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestal;
import me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalItem;
import me.mykindos.betterpvp.core.block.impl.smelter.Smelter;
import me.mykindos.betterpvp.core.block.impl.smelter.SmelterItem;
import me.mykindos.betterpvp.core.block.impl.workbench.Workbench;
import me.mykindos.betterpvp.core.block.impl.workbench.WorkbenchItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.Map;

@Singleton
public class CoreBlockBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private CraftingRecipeRegistry craftingRegistry;
    @Inject private MinecraftCraftingRecipeAdapter adapter;
    @Inject private Core core;
    @Inject private SmartBlockRegistry smartBlockRegistry;
    @Inject private Workbench workbench;
    @Inject private WorkbenchItem workbenchItem;
    @Inject private Smelter smelter;
    @Inject private SmelterItem smelterItem;
    @Inject private Anvil anvil;
    @Inject private AnvilItem anvilItem;
    @Inject private ImbuementPedestal imbuementPedestal;
    @Inject private ImbuementPedestalItem imbuementPedestalItem;

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        smartBlockRegistry.registerBlock(workbench);
        itemRegistry.registerItem(new NamespacedKey(core, "workbench"), workbenchItem);

        smartBlockRegistry.registerBlock(smelter);
        itemRegistry.registerItem(new NamespacedKey(core, "smelter"), smelterItem);

        smartBlockRegistry.registerBlock(anvil);
        registerFallbackItem(itemRegistry, "core:anvil", Material.ANVIL, anvilItem, false);

        smartBlockRegistry.registerBlock(imbuementPedestal);
        itemRegistry.registerItem(new NamespacedKey(core, "imbuement_pedestal"), imbuementPedestalItem);
    }


    private void registerFallbackItem(ItemRegistry itemRegistry, String key, Material material, BaseItem item, boolean keepRecipe) {
        final NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        itemRegistry.registerFallbackItem(namespacedKey, material, item);
        final List<Recipe> old = Bukkit.getRecipesFor(ItemStack.of(material));
        if (old.isEmpty()) {
            return;
        }

        final Map<NamespacedKey, CraftingRecipe> disabled = adapter.disableRecipesFor(material);
        if (keepRecipe) {
            for (Map.Entry<NamespacedKey, CraftingRecipe> entry : disabled.entrySet()) {
                craftingRegistry.registerRecipe(entry.getKey(), entry.getValue());
            }
        }
    }

}
