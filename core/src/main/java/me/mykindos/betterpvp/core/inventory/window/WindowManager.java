package me.mykindos.betterpvp.core.inventory.window;

import me.mykindos.betterpvp.core.inventory.InvUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages all {@link me.mykindos.betterpvp.core.inventory.window.Window Windows} and provides methods for searching them.
 */
public class WindowManager implements Listener {

    private static WindowManager instance;

    private final Map<Inventory, AbstractWindow> windowsByInventory = new HashMap<>();
    private final Map<Player, AbstractWindow> windowsByPlayer = new HashMap<>();


    private WindowManager() {
        Plugin plugin = InvUI.getInstance().getPlugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        InvUI.getInstance().addDisableHandler(() -> new HashSet<>(windowsByPlayer.values()).forEach(AbstractWindow::close));
    }

    /**
     * Gets the {@link WindowManager} instance or creates a new one if there isn't one.
     *
     * @return The {@link WindowManager} instance
     */
    public static WindowManager getInstance() {
        return instance == null ? instance = new WindowManager() : instance;
    }

    /**
     * Adds an {@link AbstractWindow} to the list of windows.
     * This method is usually called by the {@link me.mykindos.betterpvp.core.inventory.window.Window} itself.
     *
     * @param window The {@link AbstractWindow} to add
     */
    public void addWindow(AbstractWindow window) {
        windowsByInventory.put(window.getInventories()[0], window);
        windowsByPlayer.put(window.getViewer(), window);
    }

    /**
     * Removes an {@link AbstractWindow} from the list of windows.
     * This method is usually called by the {@link me.mykindos.betterpvp.core.inventory.window.Window} itself.
     *
     * @param window The {@link AbstractWindow} to remove
     */
    public void removeWindow(AbstractWindow window) {
        windowsByInventory.remove(window.getInventories()[0]);
        windowsByPlayer.remove(window.getViewer());
    }

    /**
     * Finds the {@link me.mykindos.betterpvp.core.inventory.window.Window} to an {@link Inventory}.
     *
     * @param inventory The {@link Inventory}
     * @return The {@link me.mykindos.betterpvp.core.inventory.window.Window} that belongs to that {@link Inventory}
     */
    @Nullable
    public me.mykindos.betterpvp.core.inventory.window.Window getWindow(Inventory inventory) {
        return windowsByInventory.get(inventory);
    }

    /**
     * Gets the {@link me.mykindos.betterpvp.core.inventory.window.Window} the {@link Player} has currently open.
     *
     * @param player The {@link Player}
     * @return The {@link me.mykindos.betterpvp.core.inventory.window.Window} the {@link Player} has currently open
     */
    @Nullable
    public me.mykindos.betterpvp.core.inventory.window.Window getOpenWindow(Player player) {
        return windowsByPlayer.get(player);
    }

    /**
     * Gets a set of all open {@link me.mykindos.betterpvp.core.inventory.window.Window Windows}.
     *
     * @return A set of all {@link me.mykindos.betterpvp.core.inventory.window.Window Windows}
     */
    public Set<me.mykindos.betterpvp.core.inventory.window.Window> getWindows() {
        return new HashSet<>(windowsByInventory.values());
    }

    /**
     * Gets a set of all open {@link me.mykindos.betterpvp.core.inventory.window.Window Windows}.
     *
     * @deprecated Use {@link #getWindows()} instead
     */
    @Deprecated
    public Set<me.mykindos.betterpvp.core.inventory.window.Window> getOpenWindows() {
        return getWindows();
    }

    @EventHandler
    private void handleInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        AbstractWindow window = (AbstractWindow) getOpenWindow(player);
        if (window != null) {
            window.handleClickEvent(event);

            if (event.getClick().name().equals("SWAP_OFFHAND") && event.isCancelled()) {
                EntityEquipment equipment = event.getWhoClicked().getEquipment();
                equipment.setItemInOffHand(equipment.getItemInOffHand());
            }
        }
    }

    @EventHandler
    private void handleInventoryDrag(InventoryDragEvent event) {
        AbstractWindow window = (AbstractWindow) getOpenWindow((Player) event.getWhoClicked());
        if (window != null) {
            window.handleDragEvent(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void handleInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        AbstractWindow window = (AbstractWindow) getWindow(event.getInventory());
        if (window != null) {
            window.handleCloseEvent(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void handleInventoryOpen(InventoryOpenEvent event) {
        AbstractWindow window = (AbstractWindow) getWindow(event.getInventory());
        if (window != null) {
            window.handleOpenEvent(event);
        }
    }

    @EventHandler
    private void handlePlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        AbstractWindow window = (AbstractWindow) getOpenWindow(player);
        if (window != null) {
            window.handleCloseEvent(true);
        }
    }

    @EventHandler
    private void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        AbstractWindow window = (AbstractWindow) getOpenWindow(player);
        if (window != null) {
            window.handleViewerDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void handleItemPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Window window = getOpenWindow((Player) entity);
            if (window instanceof AbstractDoubleWindow)
                event.setCancelled(true);
        }
    }

}
