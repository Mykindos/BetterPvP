package me.mykindos.betterpvp.game.framework.listener.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.npc.KitSelectorUseEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.InventoryProvider;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.manager.RoleSelectorManager;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles player interactions with kit selectors
 */
@BPvPListener
@Singleton
@CustomLog
public class KitSelectorListener implements Listener {

    private final ServerController serverController;
    private final PlayerController playerController;
    private final RoleSelectorManager roleSelectorManager;
    private final ItemHandler itemHandler;
    private final HotBarLayoutManager layoutManager;
    private final InventoryProvider inventoryProvider;
    private final MappedWorld waitingLobby;
    private final MapManager manager;

    @Inject
    public KitSelectorListener(ServerController serverController, PlayerController playerController, RoleSelectorManager roleSelectorManager,
                               ItemHandler itemHandler, HotBarLayoutManager layoutManager, InventoryProvider inventoryProvider,
                               @Named("Waiting Lobby") MappedWorld waitingLobby, MapManager manager) {
        this.serverController = serverController;
        this.playerController = playerController;
        this.roleSelectorManager = roleSelectorManager;
        this.itemHandler = itemHandler;
        this.layoutManager = layoutManager;
        this.inventoryProvider = inventoryProvider;
        this.waitingLobby = waitingLobby;
        this.manager = manager;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        // Add kit selectors when entering WAITING state
        roleSelectorManager.createKitSelectors(waitingLobby, inventoryProvider, layoutManager, itemHandler); // Default when server starts

        serverController.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> {
            roleSelectorManager.createKitSelectors(waitingLobby, inventoryProvider, layoutManager, itemHandler);
        });

        serverController.getStateMachine().addExitHandler(GameState.STARTING, oldState -> {
            roleSelectorManager.clearSelectors(waitingLobby);
        });

        // Spawn kit selectors when entering IN_GAME state
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            roleSelectorManager.createKitSelectors(manager.getCurrentMap(), inventoryProvider, layoutManager, itemHandler);
        });

        // Clean up kit selectors when exiting ENDING state
        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> {
            roleSelectorManager.clearSelectors(manager.getCurrentMap());
        });
    }

    // Handle player interactions with kit selectors
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRoleSwitch(KitSelectorUseEvent event) {
        if ((serverController.getCurrentState() == GameState.IN_GAME || serverController.getCurrentState() == GameState.ENDING)
                && !playerController.getParticipant(event.getPlayer()).isAlive()) {
            event.setCancelled(true);
            return;
        }

        if (event.getKitSelector().getRole() != null) { // Ignore nulls
            roleSelectorManager.selectRole(event.getPlayer(), event.getKitSelector().getRole());
        }
    }

}