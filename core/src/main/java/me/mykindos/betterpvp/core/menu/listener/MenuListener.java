package me.mykindos.betterpvp.core.menu.listener;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.command.menus.PlayerInventoryMenu;
import me.mykindos.betterpvp.core.cooldowns.Cooldown;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.inventory.window.WindowManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@BPvPListener
@Singleton
@Slf4j
public class MenuListener implements Listener {

    @Inject
    private CooldownManager cooldownManager;

    private final List<Gui> frozen = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClickStart(InventoryClickEvent event) {
        if (!isValid(event)) {
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        final int slot = event.getSlot();
        final Window open = WindowManager.getInstance().getOpenWindow(player);

        if (!(open instanceof AbstractSingleWindow window) || event.getClickedInventory() == event.getView().getBottomInventory()) {
            return;
        }

        if (window.getInventories()[0] != event.getClickedInventory()) {
            return;
        }

        double cooldown = 0.05;
        final AbstractGui gui = window.getGui();
        final SlotElement slotElement = gui.getSlotElement(slot);
        if (slotElement instanceof SlotElement.ItemSlotElement itemSlotElement) {
            final Item item = itemSlotElement.getItem();
            if (item instanceof CooldownButton button) {
                final double toSet = button.getCooldown();
                Preconditions.checkArgument(toSet >= 0, "Cooldown must be >= 0");
                cooldown = toSet;
            }
        }

        // At this point, we know they have a window open.
        // Let's add an internal cooldown to prevent them from spamming the menu.
        Consumer<Cooldown> cooldownConsumer = cd -> frozen.remove(gui);

        if (!cooldownManager.use(player, "Button Click " + slot, cooldown, false, true, true, null, 0, cooldownConsumer)) {
            if (gui.isFrozen()) {
                return;
            }
            gui.setFrozen(true);

            frozen.add(gui);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPostClick(InventoryClickEvent event) {
        if (!isValid(event)) {
            return;
        }

        final AbstractSingleWindow window = (AbstractSingleWindow) WindowManager.getInstance().getOpenWindow(((Player) event.getWhoClicked()));
        final AbstractGui gui = window.getGui();
        if (!frozen.contains(gui)) {
            return;
        }

        gui.setFrozen(false);
    }

    @EventHandler()
    public void onInventoryInteract(InventoryClickEvent event) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), ()-> {
            if (event.getWhoClicked() instanceof Player player) {
                for (Window window : WindowManager.getInstance().getWindows()) {
                    if (window instanceof AbstractSingleWindow abstractSingleWindow) {
                        if (abstractSingleWindow.getGui() instanceof PlayerInventoryMenu playerInventory && (player == playerInventory.getPlayer())) {
                            UtilServer.runTaskLaterAsync(JavaPlugin.getPlugin(Core.class),
                                    playerInventory::updateInventories,
                                    1L);
                            return;
                        }
                    }
                }
            }
        });

    }

    @EventHandler()
    public void onInventoryDrag(InventoryDragEvent event) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), ()-> {
            if (event.getWhoClicked() instanceof Player player) {
                for (Window window : WindowManager.getInstance().getWindows()) {
                    if (window instanceof AbstractSingleWindow abstractSingleWindow) {
                        if (abstractSingleWindow.getGui() instanceof PlayerInventoryMenu playerInventory && (player == playerInventory.getPlayer())) {
                            UtilServer.runTaskLaterAsync(JavaPlugin.getPlugin(Core.class),
                                    playerInventory::updateInventories,
                                    1L);
                            return;
                        }
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        for (Window window : WindowManager.getInstance().getWindows()) {
            if (window instanceof AbstractSingleWindow abstractSingleWindow) {
                if (abstractSingleWindow.getGui() instanceof PlayerInventoryMenu playerInventory) {
                    playerInventory.playerLogin(player);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        for (Window window : WindowManager.getInstance().getWindows()) {
            if (window instanceof AbstractSingleWindow abstractSingleWindow) {
                if (abstractSingleWindow.getGui() instanceof PlayerInventoryMenu playerInventory) {
                    playerInventory.onPlayerLeave(player);
                }
            }
        }
    }

    //@UpdateEvent() Requires optimization
    //public void doFlashing() {
    //    WindowManager.getInstance().getWindows().forEach(window -> {
    //        if (window instanceof AbstractSingleWindow abstractSingleWindow) {
    //            for (SlotElement slotElement : abstractSingleWindow.getGui().getSlotElements()) {
    //                if (slotElement instanceof SlotElement.ItemSlotElement itemSlotElement) {
    //                    if (itemSlotElement.getItem() instanceof FlashingButton<?> flashingButton) {
    //                       flashingButton.handleFlash();
    //                    }
    //                }
    //            }
    //        }
    //    });
    //}

    private boolean isValid(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return false;
        }

        final Window open = WindowManager.getInstance().getOpenWindow(player);
        if (open == null) {
            return false;
        }

        final int slot = event.getSlot();
        if (slot < 0) {
            return false; // Clicked outside the inventory
        }

        if (!(open instanceof AbstractSingleWindow)) {
            return false;
        }

        return true;
    }

}
