package me.mykindos.betterpvp.core.items.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomPlayerItemEvent extends CustomEvent {
    /**
     * The player that is gaining or losing the item
     */
    private final Player player;
    /**
     * The item stack gained or lost
     */
    private final ItemStack item;
    /**
     * The BPvPItem of this itemstack. Null if the item is not a BPvPItemStack
     */
    @Nullable
    private final BPvPItem bPvPItem;
    /**
     * The previous inventory the item was in. Null if was not in an inventory
     */
    @Nullable
    private final Inventory previousInventory;
    /**
     * The inventory the item is now in. Null if dropped or is not in an inventory
     */
    @Nullable
    private final Inventory inventory;
    /**
     * The status of this event, either Gain or Lose
     */
    private final ItemStatus itemStatus;
}
