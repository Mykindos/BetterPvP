package me.mykindos.betterpvp.core.framework.events.scoreboard;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;


@EqualsAndHashCode(callSuper = true)
@Data
public class ScoreboardUpdateEvent extends CustomCancellableEvent {

    private final Player player;

}
