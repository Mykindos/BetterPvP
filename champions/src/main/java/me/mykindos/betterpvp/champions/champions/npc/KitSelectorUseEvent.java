package me.mykindos.betterpvp.champions.champions.npc;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Value
public class KitSelectorUseEvent extends CustomCancellableEvent {
    Player player;
    KitSelector kitSelector;
}
