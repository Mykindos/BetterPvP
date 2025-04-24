package me.mykindos.betterpvp.game.framework.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

/**
 * Handles giving inventories to people
 */
@Singleton
public class InventoryProvider {

    private final ServerController serverController;
    private final RoleSelectorManager roleSelectorManager;
    private final ItemHandler itemHandler;
    private final PlayerController playerController;
    private final HotBarLayoutManager layoutManager;

    @Inject
    public InventoryProvider(ServerController serverController, RoleSelectorManager roleSelectorManager, ItemHandler itemHandler, PlayerController playerController, HotBarLayoutManager layoutManager) {
        this.serverController = serverController;
        this.roleSelectorManager = roleSelectorManager;
        this.itemHandler = itemHandler;
        this.playerController = playerController;
        this.layoutManager = layoutManager;
    }

    public void refreshInventory(Player player) {
        final boolean waiting = serverController.getCurrentState() == GameState.WAITING || serverController.getCurrentState() == GameState.STARTING;

        // Clear inventory first
        final PlayerInventory inventory = player.getInventory();
        inventory.clear();

        if (waiting || playerController.getParticipant(player).isAlive()) {
            // Equip their kit
            Role role = roleSelectorManager.getRole(player);
            role.equip(itemHandler, player, true); // Only armor

            // Update hotbar
            layoutManager.applyPlayerLayout(player);
        }

        // todo: Then set their hot bar buttons ONLY in waiting state
        //  maybe a settings button
    }

}
