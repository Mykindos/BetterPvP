package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.AbstractWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.inventory.window.WindowManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

@Singleton
public class Workbench extends SmartBlock implements Listener, NexoBlock, DataHolder<WorkbenchData> {

    private final CraftingManager craftingManager;
    private final ItemFactory itemFactory;
    private final SmartBlockDataSerializer<WorkbenchData> serializer;

    @Inject
    private Workbench(CraftingManager craftingManager, ItemFactory itemFactory, Core core) {
        super("workbench", "Workbench");
        this.craftingManager = craftingManager;
        this.serializer = new StorageBlockDataSerializer<>(WorkbenchData.class, itemFactory, WorkbenchData::new);
        this.itemFactory = itemFactory;
        Bukkit.getPluginManager().registerEvents(this, core);
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
        final GuiWorkbench gui = new GuiWorkbench(player, craftingManager, itemFactory, blockInstance);
        gui.show(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final Window window = WindowManager.getInstance().getOpenWindow(player);
        if (!(window instanceof AbstractSingleWindow singleWindow)) {
            return;
        }

        if (singleWindow.getGui() instanceof GuiWorkbench guiWorkbench) {
            guiWorkbench.updateQuickCrafts();
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        final Window window = WindowManager.getInstance().getOpenWindow(event.getPlayer());
        if (!(window instanceof AbstractSingleWindow singleWindow)) {
            return;
        }

        if (singleWindow.getGui() instanceof GuiWorkbench guiWorkbench) {
            guiWorkbench.updateQuickCrafts();
        }
    }

    @Override
    public @NotNull String getId() {
        return "workbench";
    }
}
