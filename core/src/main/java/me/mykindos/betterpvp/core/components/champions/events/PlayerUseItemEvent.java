package me.mykindos.betterpvp.core.components.champions.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerUseItemEvent extends CustomCancellableEvent {

    private final Player player;
    private final ItemInstance weapon;
    private final boolean isDangerous;
}
