package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
public class Workbench extends SmartBlock implements NexoBlock, DataHolder<WorkbenchData> {

    private final CraftingManager craftingManager;
    private final ItemFactory itemFactory;
    private final SmartBlockDataSerializer<WorkbenchData> serializer;

    @Inject
    private Workbench(CraftingManager craftingManager, ItemFactory itemFactory) {
        super("workbench", "Workbench");
        this.craftingManager = craftingManager;
        this.serializer = new StorageBlockDataSerializer<>(WorkbenchData.class, itemFactory, WorkbenchData::new);
        this.itemFactory = itemFactory;
    }

    @Override
    public SmartBlockDataSerializer<WorkbenchData> getDataSerializer() {
        return serializer;
    }

    @Override
    public WorkbenchData createDefaultData() {
        return new WorkbenchData();
    }

    @Override
    public Class<WorkbenchData> getDataType() {
        return WorkbenchData.class;
    }

    @Override
    public boolean handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player, @NotNull Action action) {
        if (!action.isRightClick()) {
            return false; // Only handle right-click actions
        }

        // If they're holding a blueprint and they shift-click, add it to the storage
        ItemStack handStack = player.getEquipment().getItemInMainHand();
        final ItemInstance hand = itemFactory.fromItemStack(handStack).orElse(null);
        if (hand != null && hand.getBaseItem() instanceof BlueprintItem) {
            final SmartBlockData<WorkbenchData> blockData = blockInstance.getBlockData();
            blockData.update(storage -> {
                storage.addItem(hand);

                // Added successfully, so remove it from the player's hand
                player.getEquipment().setItemInMainHand(null);
                new SoundEffect(Sound.UI_LOOM_TAKE_RESULT, 0.9f, 1.0f).play(player);
            });
            return true;
        }

        // Otherwise, open the workbench GUI
        final GuiWorkbench gui = new GuiWorkbench(craftingManager, itemFactory, blockInstance);
        gui.show(player);
        return true;
    }

    @Override
    public @NotNull String getId() {
        return "workbench";
    }
}
