package me.mykindos.betterpvp.core.components.clans.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ClanEvent<T extends IClan> extends CustomCancellableEvent {

    private final Player player;
    private final T clan;
    private final boolean globalScoreboardUpdate;


}
