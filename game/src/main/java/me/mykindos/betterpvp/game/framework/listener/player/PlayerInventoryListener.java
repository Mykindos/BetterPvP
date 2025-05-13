package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.event.ChampionsBuildLoadedEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.champions.champions.npc.KitSelectorUseEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.listener.player.event.ParticipantRespawnEvent;
import me.mykindos.betterpvp.game.framework.manager.InventoryProvider;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantDeathEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles player interactions with their hot bar, in the WAITING LOBBY
 */
@PluginAdapter("Champions")
@BPvPListener
@Singleton
public class PlayerInventoryListener implements Listener {

    private final ServerController serverController;
    private final PlayerController playerController;
    private final InventoryProvider inventoryProvider;

    @Inject
    public PlayerInventoryListener(ServerController serverController, PlayerController playerController, InventoryProvider inventoryProvider) {
        this.playerController = playerController;
        this.serverController = serverController;
        this.inventoryProvider = inventoryProvider;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            playerController.getEverybody().forEach((player, participant) -> {
                inventoryProvider.refreshInventory(player); // restock everyone
            });
        });

        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> {
            playerController.getEverybody().forEach((player, participant) -> {
                inventoryProvider.refreshInventory(player); // restock everyone
            });
        });
    }

    // MONITOR because this loads after the HotbarLayoutManager has loaded their hotbar
    // (loaded on ChampionsBuildLoadedEvent @ NORMAL in PlayerControllerListener)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBuildLoad(ChampionsBuildLoadedEvent event) {
        inventoryProvider.refreshInventory(event.getPlayer());
    }

    // Reset a player's inventory when they switch kit
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRoleSwitch(KitSelectorUseEvent event) {
        inventoryProvider.refreshInventory(event.getPlayer());
    }

    // Reset a player's inventory when they switch build
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBuildApply(ApplyBuildEvent event) {
        inventoryProvider.refreshInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBuildDelete(DeleteBuildEvent event) {
        inventoryProvider.refreshInventory(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(ParticipantRespawnEvent event) {
        inventoryProvider.refreshInventory(event.getPlayer());
    }

    @EventHandler
    public void onPlayerStopSpectating(ParticipantStopSpectatingEvent event) {
        inventoryProvider.refreshInventory(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(ParticipantDeathEvent event) {
        inventoryProvider.refreshInventory(event.getPlayer());
    }

}
