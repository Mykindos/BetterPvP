package me.mykindos.betterpvp.core.framework.events.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.LootContext;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpecialItemLootEvent extends CustomEvent {

    @Nullable private final LootContext lootContext;
    private final Location location;
    private final ItemInstance itemInstance;
    private final String source;

    /**
     * We *need* the context to be non-null. For now this solves incompatibility issues with Fishing loot tables
     * not complying with the new loot system.
     * @deprecated Use {@link SpecialItemLootEvent#SpecialItemLootEvent(LootContext, ItemInstance, String)} instead
     */
    @Deprecated(forRemoval = true)
    public SpecialItemLootEvent(Location location, ItemInstance itemInstance, String source) {
        this.lootContext = null;
        this.location = location;
        this.itemInstance = itemInstance;
        this.source = source;
    }

    public SpecialItemLootEvent(LootContext lootContext, ItemInstance itemInstance, String source) {
        this.lootContext = lootContext;
        this.itemInstance = itemInstance;
        this.source = source;
        this.location = lootContext.getLocation();
    }

}
