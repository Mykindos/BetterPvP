package me.mykindos.betterpvp.champions.champions.builds.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public class LoadBuildsEvent extends CustomEvent {

    private final Player player;
    private final GamerBuilds gamerBuilds;

}
