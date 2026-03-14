package me.mykindos.betterpvp.core.components.store.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerMountEvent extends CustomCancellableEvent {

    private final Player player;

}
