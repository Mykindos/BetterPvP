package me.mykindos.betterpvp.core.block.impl.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeRegistry;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
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
public class ImbuementPedestal extends SmartBlock implements NexoBlock, DataHolder<ImbuementPedestalData> {

    private final ItemFactory itemFactory;
    private final ImbuementRecipeRegistry imbuementRecipeRegistry;
    private final ImbuementPedestalDataSerializer dataSerializer;

    @Inject
    private ImbuementPedestal(ItemFactory itemFactory, ImbuementRecipeRegistry imbuementRecipeRegistry) {
        super("imbuement_pedestal", "Imbuement Pedestal");
        this.itemFactory = itemFactory;
        this.imbuementRecipeRegistry = imbuementRecipeRegistry;
        this.dataSerializer = new ImbuementPedestalDataSerializer(itemFactory, imbuementRecipeRegistry);
    }

    @Override
    public Class<ImbuementPedestalData> getDataType() {
        return ImbuementPedestalData.class;
    }

    @Override
    public SmartBlockDataSerializer<ImbuementPedestalData> getDataSerializer() {
        return dataSerializer;
    }

    @Override
    public ImbuementPedestalData createDefaultData() {
        return new ImbuementPedestalData(itemFactory, imbuementRecipeRegistry);
    }

    @Override
    public boolean handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player, @NotNull Action action) {
        final ImbuementPedestalData data = Objects.requireNonNull(blockInstance.getData());

        // Set pedestal location if not already set
        if (data.getPedestalLocation() == null) {
            data.setPedestalLocation(blockInstance.getLocation());
            data.refreshDisplayEntities();
        }

        // Handle item removal (left-click)
        if (action.isLeftClick()) {
            return handleItemRemoval(data, player);
        }

        // Only handle right-clicks for placement and execution
        if (!action.isRightClick()) {
            return false;
        }

        final ItemStack itemInHand = player.getEquipment().getItemInMainHand();

        // Handle empty hand clicks - try to execute recipe
        if (itemInHand.getType().isAir()) {
            return handleRecipeExecution(data, player);
        }

        // Handle item placement if player has an item
        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(itemInHand);
        return itemOpt.filter(itemInstance -> handleItemPlacement(data, player, itemInstance)).isPresent();
    }

    /**
     * Handles recipe execution when player right-clicks with empty hand.
     */
    private boolean handleRecipeExecution(@NotNull ImbuementPedestalData data, @NotNull Player player) {
        if (data.getItemManager().getCurrentRecipe() == null) {
            if (data.hasItems()) {
                new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f).play(player);
            } else {
                new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f).play(player);
            }
            return false;
        }

        if (data.getRecipeExecutor().isExecutingRecipe()) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f).play(player);
            return false;
        }

        // Execute the recipe
        if (data.executeRecipe(player)) {
            return true;
        }

        return false;
    }

    /**
     * Handles removing items from the pedestal.
     */
    private boolean handleItemRemoval(@NotNull ImbuementPedestalData data, @NotNull Player player) {
        if (!data.hasItems()) {
            new SoundEffect(Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.5f).play(player);
            return false;
        }

        // Can't remove items during recipe execution
        if (data.getRecipeExecutor().isExecutingRecipe()) {
            new SoundEffect(Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.5f).play(player);
            return false;
        }

        ItemInstance removedItem = data.removeLastItem();
        if (removedItem != null) {
            // Give the item back to the player
            ItemStack itemStack = removedItem.createItemStack();
            UtilItem.insert(player, itemStack);
            new SoundEffect(Sound.BLOCK_ANVIL_HIT, 0.8f, 1.0f).play(player);
            return true;
        }

        return false;
    }

    /**
     * Handles placing items on the pedestal.
     */
    private boolean handleItemPlacement(@NotNull ImbuementPedestalData data, @NotNull Player player, @NotNull ItemInstance itemInstance) {
        // Check if pedestal is full
        if (data.isFull()) {
            UtilMessage.message(player, "Imbuement", "<red>This pedestal is full!");
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f).play(player);
            return false;
        }

        // Check if recipe is being executed
        if (data.getRecipeExecutor().isExecutingRecipe()) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f).play(player);
            return false;
        }

        // Add the item to the pedestal
        if (data.addItem(itemInstance)) {
            // Remove the item from player's hand
            player.getEquipment().setItemInMainHand(ItemStack.empty());
            new SoundEffect(Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.5f).play(player);
            return true;
        }

        return false;
    }


    @Override
    public @NotNull String getId() {
        return "imbuement_pedestal";
    }
} 