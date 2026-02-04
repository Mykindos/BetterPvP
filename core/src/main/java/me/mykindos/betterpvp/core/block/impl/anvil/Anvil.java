package me.mykindos.betterpvp.core.block.impl.anvil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.impl.Hammer;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@Singleton
public class Anvil extends SmartBlock implements NexoBlock, DataHolder<AnvilData> {

    private final ItemFactory itemFactory;
    private final ClientManager clientManager;
    private final AnvilRecipeRegistry anvilRecipeRegistry;
    private final AnvilDataSerializer dataSerializer;

    @Inject
    private Anvil(ItemFactory itemFactory, AnvilRecipeRegistry anvilRecipeRegistry, ClientManager clientManager) {
        super("anvil", "Anvil");
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        this.anvilRecipeRegistry = anvilRecipeRegistry;
        this.dataSerializer = new AnvilDataSerializer(itemFactory, anvilRecipeRegistry, clientManager);
    }

    @Override
    public Class<AnvilData> getDataType() {
        return AnvilData.class;
    }

    @Override
    public SmartBlockDataSerializer<AnvilData> getDataSerializer() {
        return dataSerializer;
    }

    @Override
    public AnvilData createDefaultData() {
        return new AnvilData(itemFactory, anvilRecipeRegistry, clientManager);
    }

    @Override
    public boolean handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player, @NotNull Action action) {
        final AnvilData data = Objects.requireNonNull(blockInstance.getData());

        // Set anvil location if not already set
        if (data.getAnvilLocation() == null) {
            data.setAnvilLocation(blockInstance.getLocation());
            data.refreshDisplayEntities();
        }

        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(player.getEquipment().getItemInMainHand());

        // Handle item removal
        if (action.isLeftClick()) {
            return handleItemRemoval(data, player);
        }

        // Handle hammer swings (diamond pickaxe)
        if (itemOpt.isPresent()) {
            final ItemInstance itemInHand = itemOpt.get();
            if (itemInHand.getBaseItem() instanceof Hammer) {
                handleHammerSwing(data, player, blockInstance);
            } else {
                // Handle item placement (any other item)
                handleItemPlacement(data, player, itemInHand);
            }
            return true;
        }
        return false;
    }

    /**
     * Handles hammer swings on the anvil.
     */
    private void handleHammerSwing(@NotNull AnvilData data, @NotNull Player player, @NotNull SmartBlockInstance blockInstance) {
        if (!data.canSwing()) {
            return;
        }

        if (!data.hasItems()) {
            new SoundEffect(Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.5f).play(player);
            return;
        }

        if (data.getItemManager().getCurrentRecipe() == null) {
            new SoundEffect(Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.5f).play(player);
            return;
        }

        // Execute the hammer swing
        data.executeHammerSwing(player, blockInstance.getLocation().add(0, 1.1, 0));
    }

    /**
     * Handles placing items on the anvil.
     */
    private void handleItemPlacement(@NotNull AnvilData data, @NotNull Player player, @NotNull ItemInstance itemInstance) {
        // Check if anvil is full
        if (data.isFull()) {
            UtilMessage.message(player, "Anvil", "<red>This anvil is full!");
            new SoundEffect(Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.5f).play(player);
            return;
        }

        // Add the item to the anvil
        if (data.addItem(itemInstance)) {
            // Remove the item from player's hand
            player.getEquipment().setItemInMainHand(ItemStack.empty());
            new SoundEffect(Sound.BLOCK_ANVIL_HIT, 0.8f, 1.0f).play(player);
            new SoundEffect(Sound.BLOCK_ANVIL_HIT, 1.2f, 1.0f).play(player);
        }
    }

    /**
     * Handles removing items from the anvil.
     */
    private boolean handleItemRemoval(@NotNull AnvilData data, @NotNull Player player) {
        if (!data.hasItems()) {
            new SoundEffect(Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.5f).play(player);
            return false;
        }

        ItemInstance removedItem = data.removeLastItem();
        if (removedItem != null) {
            // Give the item back to the player
            ItemStack itemStack = removedItem.createItemStack();
            UtilItem.insert(player, itemStack);
            new SoundEffect(Sound.BLOCK_ANVIL_HIT, 0.8f, 1.0f).play(player);
            new SoundEffect(Sound.BLOCK_ANVIL_HIT, 1.2f, 1.0f).play(player);
            return true;
        }

        return false;
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_anvil";
    }
} 