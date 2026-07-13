package me.mykindos.betterpvp.core.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableItem;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDManager;
import me.mykindos.betterpvp.core.item.impl.MirrorOfKalandra;
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
    private final UUIDManager uuidManager;
    private final Core core;

    @Inject
    private ImbuementRecipeBootstrap(ItemRegistry itemRegistry, ItemFactory itemFactory, ImbuementRecipeRegistry imbuementRecipeRegistry, UUIDManager uuidManager, Core core) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.imbuementRecipeRegistry = imbuementRecipeRegistry;
        this.uuidManager = uuidManager;
        this.core = core;
    }

    public void register() {
        registerMirrorRecipe();

        final Reflections reflections = new Reflections(PACKAGE);
        final Set<Class<? extends SocketableItem>> subTypes = reflections.getSubTypesOf(SocketableItem.class);
        final List<SocketableItem> socketables = new ArrayList<>();
        for (Class<? extends SocketableItem> runeClazz : subTypes) {
            if (runeClazz.isInterface() || Modifier.isAbstract(runeClazz.getModifiers())) {
                continue;
            }

            socketables.add(core.getInjector().getInstance(runeClazz));
        }

        for (BaseItem alreadyRegistered : itemRegistry.getItems().values()) {
            registerRecipe(itemRegistry, alreadyRegistered, socketables);
        }

        itemRegistry.addRegisterCallback((key, item) -> registerRecipe(itemRegistry, item, socketables));
    }

    private void registerMirrorRecipe() {
        final MirrorOfKalandra mirror = core.getInjector().getInstance(MirrorOfKalandra.class);
        final NamespacedKey key = new NamespacedKey("core", "mirror_of_kalandra_duplication");
        imbuementRecipeRegistry.registerRecipe(key,
                new MirrorOfKalandraImbuementRecipe(itemFactory, mirror, uuidManager, itemRegistry));
    }

    private void registerRecipe(ItemRegistry itemRegistry, BaseItem baseItem, List<SocketableItem> socketables) {
        final Optional<SocketableContainerComponent> containerOpt = baseItem.getComponent(SocketableContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return;
        }

        final NamespacedKey itemKey = Objects.requireNonNull(itemRegistry.getKey(baseItem));
        for (SocketableItem socketableItem : socketables) {
            if (socketableItem.getSocketable().canApply(baseItem)) {
                final NamespacedKey socketableKey = Objects.requireNonNull(itemRegistry.getKey(socketableItem));
                final String key = itemKey.getKey() + "_" + socketableKey.getKey();
                final NamespacedKey combinationKey = new NamespacedKey(itemKey.getNamespace(), key);
                imbuementRecipeRegistry.registerRecipe(combinationKey, new SocketableImbuementRecipe(itemFactory, baseItem, socketableItem));
            }
        }
    }
} 