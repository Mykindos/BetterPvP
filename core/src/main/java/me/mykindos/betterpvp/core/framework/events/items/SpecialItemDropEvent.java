package me.mykindos.betterpvp.core.framework.events.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Item;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpecialItemDropEvent extends CustomEvent {

    private final Item item;
    private final String source;

}
