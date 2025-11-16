package me.mykindos.betterpvp.core.item;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Map;
import java.util.Set;

@CustomLog
public final class ItemLoader {

    private final ItemRegistry itemRegistry;
    private final CraftingRecipeRegistry craftingRecipeRegistry;
    private final MinecraftCraftingRecipeAdapter adapter;
    private final BPvPPlugin plugin;

    public ItemLoader(BPvPPlugin plugin) {
        this.itemRegistry = plugin.getInjector().getInstance(ItemRegistry.class);
        this.adapter = plugin.getInjector().getInstance(MinecraftCraftingRecipeAdapter.class);
        this.craftingRecipeRegistry = plugin.getInjector().getInstance(CraftingRecipeRegistry.class);
        this.plugin = plugin;
    }

    public void load(Adapters adapters, Set<Class<?>> items) {
        for (Class<?> itemClazz : items) {
            if (!BaseItem.class.isAssignableFrom(itemClazz)) {
                log.warn("Could not load item " + itemClazz.getSimpleName() + "! Does not extend BaseItem!").submit();
                continue;
            }

            if (!adapters.canLoad(itemClazz)) {
                log.warn("Could not load item " + itemClazz.getSimpleName() + "! Dependencies not found!").submit();
                continue;
            }

            final ItemKey itemKey = itemClazz.getAnnotation(ItemKey.class);
            final NamespacedKey namespacedKey = NamespacedKey.fromString(itemKey.value());

            if (namespacedKey == null) {
                log.warn("Could not load item " + itemClazz.getSimpleName() + "! Invalid NamespacedKey: " + itemKey.value()).submit();
                continue;
            }

            //noinspection unchecked
            final BaseItem item = plugin.getInjector().getInstance((Class<? extends BaseItem>) itemClazz);

            if (itemClazz.isAnnotationPresent(FallbackItem.class)) {
                // Fallback item
                registerFallbackItem(namespacedKey, item, itemClazz.getAnnotation(FallbackItem.class));
            } else {
                // Default item
                itemRegistry.registerItem(namespacedKey, item);
            }
        }
    }

    private void registerFallbackItem(NamespacedKey namespacedKey, BaseItem item, FallbackItem annotation) {
        registerFallbackItem(namespacedKey, item, annotation.value(), annotation.keepRecipes());
    }

    public void registerFallbackItem(NamespacedKey namespacedKey, BaseItem baseItem, Material material, boolean keepRecipe) {
        itemRegistry.registerFallbackItem(namespacedKey, material, baseItem);
        final Map<NamespacedKey, CraftingRecipe> disabled = adapter.disableRecipesFor(material);
        if (keepRecipe) {
            for (Map.Entry<NamespacedKey, CraftingRecipe> entry : disabled.entrySet()) {
                craftingRecipeRegistry.registerRecipe(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<NamespacedKey, CraftingRecipe> entry : disabled.entrySet()) {
                craftingRecipeRegistry.clearRecipe(entry.getKey());
            }
        }
    }

}
