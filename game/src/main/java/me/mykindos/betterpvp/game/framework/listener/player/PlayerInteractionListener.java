package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.PlayerInteractionSettings;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.PlayerInventory;

@BPvPListener
@Singleton
public class PlayerInteractionListener implements Listener {

    private final ServerController serverController;

    @Inject
    public PlayerInteractionListener(ServerController serverController) {
        this.serverController = serverController;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof PlayerInventory)) {
            return;
        }

        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> event.setCancelled(true);
            case IN_GAME, ENDING -> {
                if (!getInteractionSettings().isInventoryClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> event.setCancelled(true);
            case IN_GAME, ENDING -> {
                if (!getInteractionSettings().isBlockBreak()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> event.setCancelled(true);
            case IN_GAME, ENDING -> {
                if (!getInteractionSettings().isBlockPlace()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> event.setCancelled(true);
            case IN_GAME, ENDING -> {
                if (!getInteractionSettings().isItemDrop()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> event.setCancelled(true);
            case IN_GAME, ENDING -> {
                if (!getInteractionSettings().isBlockInteract()) {
                    event.setUseItemInHand(Event.Result.DEFAULT);
                    event.setUseInteractedBlock(Event.Result.DENY);
                }
            }
        }
    }

    @EventHandler
    public void onDurability(DamageEvent event) {
        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> event.setCancelled(true);
            case IN_GAME, ENDING -> {
                if (!getInteractionSettings().isItemDurability()) {
                    event.getDurabilityParameters().disableAllDurability();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDurabilityGeneric(PlayerItemDamageEvent event) {
        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> event.setCancelled(true);
            case IN_GAME, ENDING -> {
                if (!getInteractionSettings().isItemDurability()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private PlayerInteractionSettings getInteractionSettings() {
        return serverController.getCurrentGame().getConfiguration().getInteractionSettings();
    }

    @EventHandler
    public void onSkillUse(PlayerUseSkillEvent event) {
        if (serverController.getCurrentState() == GameState.WAITING || serverController.getCurrentState() == GameState.STARTING) {
            event.setCancelled(true); // Cancel skill use if in waiting lobby
        }
    }

}
