package me.mykindos.betterpvp.core.item.attunement;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Value
public class PlayerAttuneItemEvent extends CustomCancellableEvent {

    Player player;
    ItemInstance itemInstance;
    PurityComponent purityComponent;

}
