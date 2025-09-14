package me.mykindos.betterpvp.clans.clans.explosion;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.Objects;

@PluginAdapter("Clans")
@Singleton
public class ExplosiveResistanceBootstrap {

    private final ItemRegistry itemRegistry;
    private final CraftingRecipeRegistry craftingRegistry;
    private final MinecraftCraftingRecipeAdapter adapter;

    @Inject
    private ExplosiveResistanceBootstrap(ItemRegistry itemRegistry, CraftingRecipeRegistry craftingRegistry, MinecraftCraftingRecipeAdapter adapter) {
        this.itemRegistry = itemRegistry;
        this.craftingRegistry = craftingRegistry;
        this.adapter = adapter;
    }

    @Inject
    private void registerExplosiveResistantBlocks() {
        for (ExplosiveResistantBlocks tree : ExplosiveResistantBlocks.values()) {
            // Hardest -> Weakest
            final List<Material> tiers = tree.getTiers();
            Preconditions.checkState(tiers.size() > 1, "Explosive resistance tree must have at least 2 tiers");
            final Material first = tiers.getFirst();
            final TranslatableComponent name = Component.translatable(first.translationKey());

            final int treeSize = tiers.size();
            for (int i = 0; i < treeSize; i++) {
                int explosiveResistance = treeSize - (i + 1);
                final Material material = tiers.get(i);
                final VanillaItem item = new VanillaItem(name, material, ItemRarity.COMMON);
                item.addBaseComponent(new ExplosiveResistanceComponent(explosiveResistance));
                registerFallbackItem(material.translationKey(), material, item, true);
            }
        }
    }

    private void registerFallbackItem(String key, Material material, BaseItem item, boolean keepRecipe) {
        itemRegistry.registerFallbackItem(new NamespacedKey("minecraft", key), material, item);
        if (keepRecipe) {
            final Recipe old = Bukkit.getRecipe(material.getKey());
            if (old == null) return;
            final CraftingRecipe craftingRecipe = adapter.convertToCustomRecipe(old);
            if (craftingRecipe != null) craftingRegistry.registerRecipe(craftingRecipe);
        }
    }

}
