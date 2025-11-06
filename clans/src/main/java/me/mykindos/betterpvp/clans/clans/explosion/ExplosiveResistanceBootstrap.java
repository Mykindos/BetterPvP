package me.mykindos.betterpvp.clans.clans.explosion;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.Map;

@Singleton
public class ExplosiveResistanceBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private CraftingRecipeRegistry craftingRegistry;
    @Inject private Provider<MinecraftCraftingRecipeAdapter> adapter;

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

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
                registerFallbackItem(itemRegistry, material.getKey().toString(), material, item, true);
            }
        }
    }

    private void registerFallbackItem(ItemRegistry itemRegistry, String key, Material material, BaseItem item, boolean keepRecipe) {
        final NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        itemRegistry.registerFallbackItem(namespacedKey, material, item);
        final List<Recipe> old = Bukkit.getRecipesFor(ItemStack.of(material));
        if (old.isEmpty()) {
            return;
        }

        final Map<NamespacedKey, CraftingRecipe> disabled = adapter.get().disableRecipesFor(material);
        if (keepRecipe) {
            for (Map.Entry<NamespacedKey, CraftingRecipe> entry : disabled.entrySet()) {
                craftingRegistry.registerRecipe(entry.getKey(), entry.getValue());
            }
        }
    }

}
