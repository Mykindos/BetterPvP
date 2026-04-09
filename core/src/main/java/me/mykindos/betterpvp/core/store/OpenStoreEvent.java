package me.mykindos.betterpvp.core.store;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Value
public class OpenStoreEvent extends CustomCancellableEvent {

    Player player;

}
