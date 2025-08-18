package me.mykindos.betterpvp.core.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bootstrap class for registering imbuement recipes.
 * This class is responsible for setting up all imbuement crafting recipes in the system.
 */
@Singleton
@PluginAdapter("Core")
public class ImbuementRecipeBootstrap {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final ImbuementRecipeRegistry imbuementRecipeRegistry;

    @Inject
    private ImbuementRecipeBootstrap(ItemFactory itemFactory, ItemRegistry itemRegistry, ImbuementRecipeRegistry imbuementRecipeRegistry) {
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        this.imbuementRecipeRegistry = imbuementRecipeRegistry;
    }

    /**
     * Creates a namespaced key for the Core plugin.
     * @param name The key name
     * @return A namespaced key for the Core plugin
     */
    private NamespacedKey key(String name) {
        return new NamespacedKey(JavaPlugin.getPlugin(Core.class), name);
    }

    /**
     * Register imbuement recipes including the rune recipe handler.
     * This method sets up the core imbuement system.
     */
    @Inject
    private void registerRecipes() {
        // Register the rune imbuement recipe handler
        imbuementRecipeRegistry.registerRecipe(new RuneImbuementRecipe(itemFactory));
    }
} 