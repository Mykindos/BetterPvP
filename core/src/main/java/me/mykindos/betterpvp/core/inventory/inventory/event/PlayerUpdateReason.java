package me.mykindos.betterpvp.core.inventory.inventory.event;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import me.mykindos.betterpvp.core.inventory.inventory.event.UpdateReason;

public class PlayerUpdateReason implements UpdateReason {
    
    private final Player player;
    private final InventoryEvent event;
    
    public PlayerUpdateReason(Player player, InventoryEvent event) {
        this.player = player;
        this.event = event;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public InventoryEvent getEvent() {
        return event;
    }
    
}
