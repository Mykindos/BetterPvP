package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.behavior.StorageBehavior;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.storage.StorageBlockData;
import me.mykindos.betterpvp.core.block.data.storage.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
public class Workbench extends SmartBlock implements NexoBlock, DataHolder<StorageBlockData> {

    private final CraftingManager craftingManager;
    private final ItemFactory itemFactory;

    @Inject
    private Workbench(CraftingManager craftingManager, ItemFactory itemFactory) {
        super("workbench", "Workbench");
        this.craftingManager = craftingManager;
        this.itemFactory = itemFactory;
        setClickBehavior(this::handleClick);
    }

    @Override
    public SmartBlockDataSerializer<StorageBlockData> getDataSerializer() {
        return new StorageBlockDataSerializer(itemFactory);
    }

    @Override
    public StorageBlockData createDefaultData() {
        return new StorageBlockData();
    }

    @Override
    public Class<StorageBlockData> getDataType() {
        return StorageBlockData.class;
    }

    private void handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player) {
        // If they're holding a blueprint and they shift-click, add it to the storage
        ItemStack handStack = player.getEquipment().getItemInMainHand();
        final ItemInstance hand = itemFactory.fromItemStack(handStack).orElse(null);
        if (player.isSneaking() && hand != null && hand.getBaseItem() instanceof BlueprintItem) {
            final SmartBlockData<StorageBlockData> blockData = blockInstance.getBlockData();
            blockData.update(storage -> {
                storage.addItem(hand);

                // Added successfully, so remove it from the player's hand
                player.getEquipment().setItemInMainHand(null);
                new SoundEffect(Sound.UI_LOOM_TAKE_RESULT, 0.9f, 1.0f).play(player);
            });
            return;
        }

        // Otherwise, open the workbench GUI
        final GuiWorkbench gui = new GuiWorkbench(craftingManager, itemFactory, blockInstance);
        gui.show(player);
    }

    @Override
    public @NotNull String getId() {
        return "workbench";
    }
}
