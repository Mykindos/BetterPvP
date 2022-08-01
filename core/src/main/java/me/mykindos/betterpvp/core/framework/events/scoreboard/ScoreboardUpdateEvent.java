package me.mykindos.betterpvp.core.framework.events.scoreboard;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;


@EqualsAndHashCode(callSuper = true)
@Data
public class ScoreboardUpdateEvent extends CustomEvent {

    private final Player player;

}
