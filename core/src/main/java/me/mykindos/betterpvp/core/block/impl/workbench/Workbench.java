package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.inventory.window.WindowManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class Workbench extends SmartBlock implements Listener, NexoBlock, DataHolder<WorkbenchData> {

    private final CraftingManager craftingManager;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;
    private final SmartBlockDataSerializer<WorkbenchData> serializer;
    private final Core core;

    @Inject
    private Workbench(CraftingManager craftingManager, ClientManager clientManager, ItemFactory itemFactory, Core core) {
        super("workbench", "Workbench");
        this.craftingManager = craftingManager;
        this.clientManager = clientManager;
        this.serializer = new StorageBlockDataSerializer<>(WorkbenchData.class, itemFactory, WorkbenchData::new);
        this.itemFactory = itemFactory;
        this.core = core;
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

        // Open the workbench GUI
        final GuiWorkbench gui = new GuiWorkbench(player, craftingManager, itemFactory, clientManager);
        gui.show(player);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.NOTHING) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final Window window = WindowManager.getInstance().getOpenWindow(player);
        if (!(window instanceof AbstractSingleWindow singleWindow)) {
            return;
        }

        if (singleWindow.getGui() instanceof GuiWorkbench guiWorkbench) {
            queueUpdate(guiWorkbench);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        final Window window = WindowManager.getInstance().getOpenWindow(event.getPlayer());
        if (!(window instanceof AbstractSingleWindow singleWindow)) {
            return;
        }
        if (singleWindow.getGui() instanceof GuiWorkbench guiWorkbench) {
            queueUpdate(guiWorkbench);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        final Window window = WindowManager.getInstance().getOpenWindow(event.getPlayer());
        if (!(window instanceof AbstractSingleWindow singleWindow)) {
            return;
        }

        if (singleWindow.getGui() instanceof GuiWorkbench guiWorkbench) {
            queueUpdate(guiWorkbench);
        }
    }

    private void queueUpdate(GuiWorkbench workbench) {
        UtilServer.runTaskLater(core, workbench::updateQuickCrafts, 1L);
    }

    @Override
    public @NotNull String getId() {
        return "workbench";
    }
}
