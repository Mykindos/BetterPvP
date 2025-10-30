package me.mykindos.betterpvp.core.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bootstrap class for registering imbuement recipes.
 * This class is responsible for setting up all imbuement crafting recipes in the system.
 */
@Singleton
public class ImbuementRecipeBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private ItemFactory itemFactory;
    @Inject private ImbuementRecipeRegistry imbuementRecipeRegistry;
    @Inject private ScorchingRuneItem scorchingRune;
    @Inject private UnbreakingRuneItem unbreakingRune;

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
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        final List<RuneItem> runes = List.of(scorchingRune, unbreakingRune);
        for (BaseItem alreadyRegistered : itemRegistry.getItems().values()) {
            registerRecipe(itemRegistry, alreadyRegistered, runes);
        }

        itemRegistry.addRegisterCallback((key, item) -> registerRecipe(itemRegistry, item, runes));
    }

    private void registerRecipe(ItemRegistry itemRegistry, BaseItem baseItem, List<RuneItem> runes) {
        final Optional<RuneContainerComponent> containerOpt = baseItem.getComponent(RuneContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return;
        }

        final RuneContainerComponent container = containerOpt.get();
        if (!container.hasAvailableSockets()) {
            return;
        }

        final NamespacedKey itemKey = Objects.requireNonNull(itemRegistry.getKey(baseItem));
        for (RuneItem runeItem : runes) {
            if (!container.hasRune(runeItem.getRune()) && runeItem.getRune().canApply(baseItem)) {
                final NamespacedKey runeKey = Objects.requireNonNull(itemRegistry.getKey(runeItem));
                final String key = itemKey.getKey() + "_" + runeKey.getKey();
                final NamespacedKey combinationKey = new NamespacedKey(itemKey.getNamespace(), key);
                imbuementRecipeRegistry.registerRecipe(combinationKey, new RuneImbuementRecipe(itemFactory, baseItem, runeItem));
            }
        }
    }
} 