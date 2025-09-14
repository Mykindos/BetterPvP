package me.mykindos.betterpvp.core.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

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
    private void registerRecipes(ScorchingRuneItem scorchingRune, UnbreakingRuneItem unbreakingRune) {
        final List<RuneItem> runes = List.of(scorchingRune, unbreakingRune);
        for (BaseItem alreadyRegistered : itemRegistry.getItems().values()) {
            registerRecipe(alreadyRegistered, runes);
        }

        itemRegistry.addRegisterCallback((key, item) -> registerRecipe(item, runes));
    }

    private void registerRecipe(BaseItem baseItem, List<RuneItem> runes) {
        final Optional<RuneContainerComponent> containerOpt = baseItem.getComponent(RuneContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return;
        }

        final RuneContainerComponent container = containerOpt.get();
        if (!container.hasAvailableSockets()) {
            return;
        }

        for (RuneItem runeItem : runes) {
            if (!container.hasRune(runeItem.getRune()) && runeItem.getRune().canApply(baseItem)) {
                imbuementRecipeRegistry.registerRecipe(new RuneImbuementRecipe(itemFactory, baseItem, runeItem));
            }
        }
    }
} 