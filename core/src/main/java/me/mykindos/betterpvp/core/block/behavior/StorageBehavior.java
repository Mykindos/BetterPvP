package me.mykindos.betterpvp.core.block.behavior;

import io.lumine.mythic.bukkit.utils.pdc.DataType;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a behavior that allows a block to act as a storage container.
 */
public final class StorageBehavior implements SmartBlockBehavior {

    private static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "storage_content");

    private final ItemFactory itemFactory;

    public StorageBehavior(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public @NotNull List<@NotNull ItemInstance> getContent(@NotNull SmartBlockInstance instance) {
        final Block block = instance.getHandle();

        // Get the persistent data container
        final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        final ItemStack[] itemStacks = Objects.requireNonNullElse(pdc.get(KEY, DataType.ITEM_STACK_ARRAY), new ItemStack[0]);

        // Convert ItemStacks to ItemInstances
        return itemFactory.fromArray(itemStacks);
    }

    /**
     * Edit the items in the storage block.
     * @param instance the block instance to edit
     * @param contentMutator a consumer that receives the current items in the storage, allowing modification
     */
    public void edit(@NotNull SmartBlockInstance instance, Consumer<List<ItemInstance>> contentMutator) {
        final Block block = instance.getHandle();

        // Get current content
        final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        final ItemStack[] itemStacks = Objects.requireNonNullElse(pdc.get(KEY, DataType.ITEM_STACK_ARRAY), new ItemStack[0]);
        final List<ItemInstance> items = itemFactory.fromArray(itemStacks);

        // Apply the content mutator
        contentMutator.accept(items);

        // Save the modified content back to the block
        final ItemStack[] newItemStacks = items.stream()
                .map(ItemInstance::createItemStack)
                .toArray(ItemStack[]::new);
        pdc.set(KEY, DataType.ITEM_STACK_ARRAY, newItemStacks);
        UtilBlock.setPersistentDataContainer(block, pdc);
    }

}
