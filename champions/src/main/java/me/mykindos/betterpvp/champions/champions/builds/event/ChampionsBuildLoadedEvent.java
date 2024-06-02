package me.mykindos.betterpvp.champions.champions.builds.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChampionsBuildLoadedEvent extends CustomEvent {

    private final Player player;
    private final GamerBuilds gamerBuilds;

}
