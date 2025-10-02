package me.mykindos.betterpvp.core.framework.events.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.LootContext;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpecialItemLootEvent extends CustomEvent {

    private final LootContext lootContext;
    private final ItemInstance itemInstance;
    private final String source;

}
