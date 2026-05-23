package me.mykindos.betterpvp.core.framework.events.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootSource;
import org.bukkit.Location;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpecialItemLootEvent extends CustomEvent {

    private final LootContext lootContext;
    private final Location location;
    private final ItemInstance itemInstance;
    private final LootSource source;

    public SpecialItemLootEvent(LootContext lootContext, ItemInstance itemInstance, LootSource source) {
        this.lootContext = lootContext;
        this.itemInstance = itemInstance;
        this.source = source;
        this.location = lootContext.getLocation();
    }
}
