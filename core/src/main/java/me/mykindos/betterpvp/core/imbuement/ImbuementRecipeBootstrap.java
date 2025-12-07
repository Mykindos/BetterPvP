package me.mykindos.betterpvp.core.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;
import org.bukkit.NamespacedKey;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static me.mykindos.betterpvp.core.Core.PACKAGE;

/**
 * Bootstrap class for registering imbuement recipes.
 * This class is responsible for setting up all imbuement crafting recipes in the system.
 */
@Singleton
public class ImbuementRecipeBootstrap {

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final ImbuementRecipeRegistry imbuementRecipeRegistry;
    private final Core core;

    @Inject
    private ImbuementRecipeBootstrap(ItemRegistry itemRegistry, ItemFactory itemFactory, ImbuementRecipeRegistry imbuementRecipeRegistry, Core core) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.imbuementRecipeRegistry = imbuementRecipeRegistry;
        this.core = core;
    }

    public void register() {
        final Reflections reflections = new Reflections(PACKAGE);
        final Set<Class<? extends RuneItem>> subTypes = reflections.getSubTypesOf(RuneItem.class);
        final List<RuneItem> runes = new ArrayList<>();
        for (Class<? extends RuneItem> runeClazz : subTypes) {
            if (runeClazz.isInterface() || Modifier.isAbstract(runeClazz.getModifiers())) {
                continue;
            }

            runes.add(core.getInjector().getInstance(runeClazz));
        }

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

        final NamespacedKey itemKey = Objects.requireNonNull(itemRegistry.getKey(baseItem));
        for (RuneItem runeItem : runes) {
            if (runeItem.getRune().canApply(baseItem)) {
                final NamespacedKey runeKey = Objects.requireNonNull(itemRegistry.getKey(runeItem));
                final String key = itemKey.getKey() + "_" + runeKey.getKey();
                final NamespacedKey combinationKey = new NamespacedKey(itemKey.getNamespace(), key);
                imbuementRecipeRegistry.registerRecipe(combinationKey, new RuneImbuementRecipe(itemFactory, baseItem, runeItem));
            }
        }
    }
} 