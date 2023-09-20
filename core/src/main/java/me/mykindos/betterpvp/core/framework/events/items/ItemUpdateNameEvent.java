package me.mykindos.betterpvp.core.framework.events.items;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class ItemUpdateNameEvent extends CustomEvent {

    private final ItemMeta itemMeta;
    private Component itemName;

}
