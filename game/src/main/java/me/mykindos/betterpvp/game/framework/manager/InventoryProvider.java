package me.mykindos.betterpvp.game.framework.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;

/**
 * Handles giving inventories to people
 */
@Singleton
public class InventoryProvider {

    private final ServerController serverController;
    private final RoleSelectorManager roleSelectorManager;
    private final ItemFactory itemFactory;
    private final PlayerController playerController;
    private final HotBarLayoutManager layoutManager;
    private final RoleManager roleManager;

    @Inject
    public InventoryProvider(ServerController serverController, RoleSelectorManager roleSelectorManager, ItemFactory itemFactory,
                             PlayerController playerController, HotBarLayoutManager layoutManager, RoleManager roleManager) {
        this.serverController = serverController;
        this.roleSelectorManager = roleSelectorManager;
        this.itemFactory = itemFactory;
        this.playerController = playerController;
        this.layoutManager = layoutManager;
        this.roleManager = roleManager;
    }

    public void refreshInventory(Player player) {
        final boolean waiting = serverController.getCurrentState() == GameState.WAITING || serverController.getCurrentState() == GameState.STARTING;

        // Clear inventory first
        final PlayerInventory inventory = player.getInventory();
        inventory.clear();
        final InventoryView openInventory = player.getOpenInventory();
        if (openInventory.getTopInventory() instanceof CraftingInventory craftingInventory) {
            craftingInventory.clear();
        }

        final Participant participant = playerController.getParticipant(player);

        if (waiting || participant.isAlive()) {
            // Equip their kit
            Role role = roleSelectorManager.getRole(player);
            roleManager.equipRole(player, role); // Only armor

            // Update hotbar
            layoutManager.applyPlayerLayout(player);
        }

        //if a player is dead and not spectating, equip their armor to allow their skills and energy to recharge
        if (!participant.isAlive() && !participant.isSpectating()) {
            Role role = roleSelectorManager.getRole(player);
            roleManager.equipRole(player, role);
        }

        // todo: Then set their hot bar buttons ONLY in waiting state
        //  maybe a settings button
    }

}
