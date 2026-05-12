package me.mykindos.betterpvp.core.item;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Set;

@CustomLog
public final class ItemLoader {

    private final ItemRegistry itemRegistry;
    private final MinecraftCraftingRecipeAdapter adapter;
    private final BPvPPlugin plugin;

    public ItemLoader(BPvPPlugin plugin) {
        this.itemRegistry = plugin.getInjector().getInstance(ItemRegistry.class);
        this.adapter = plugin.getInjector().getInstance(MinecraftCraftingRecipeAdapter.class);
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

            try {
                //noinspection unchecked
                final BaseItem item = plugin.getInjector().getInstance((Class<? extends BaseItem>) itemClazz);

                if (itemClazz.isAnnotationPresent(FallbackItem.class)) {
                    // Fallback item
                    registerFallbackItem(namespacedKey, item, itemClazz.getAnnotation(FallbackItem.class));
                } else {
                    // Default item
                    itemRegistry.registerItem(namespacedKey, item);
                }
            } catch (Exception e) {
                log.error("Error registering an item {}", namespacedKey, e).submit();
            }

        }

        // Apply any recipe disables declared in items/recipes.yml for this plugin
        adapter.disableRecipesFromConfig(plugin);
    }

    private void registerFallbackItem(NamespacedKey namespacedKey, BaseItem item, FallbackItem annotation) {
        registerFallbackItem(namespacedKey, item, annotation.value(), annotation.keepRecipes());
    }

    public void registerFallbackItem(NamespacedKey namespacedKey, BaseItem baseItem, Material material, boolean keepRecipe) {
        itemRegistry.registerFallbackItem(namespacedKey, material, baseItem);
        // keepRecipe=true: do nothing — the ServerLoad pass in CraftingRecipeRegistry.registerMinecraftDefaults()
        // will convert this material's recipes against the final fallback registry, which is what we want.
        // keepRecipe=false: mark the recipes as disabled so the ServerLoad pass skips them.
        if (!keepRecipe) {
            adapter.disableRecipesFor(material);
        }
    }

}
