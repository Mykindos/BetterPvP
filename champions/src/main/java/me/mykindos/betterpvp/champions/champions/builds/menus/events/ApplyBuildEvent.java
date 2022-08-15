package me.mykindos.betterpvp.champions.champions.builds.menus.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplyBuildEvent extends CustomEvent {
    private final Player player;
    private final GamerBuilds builds;
    private final RoleBuild oldBuild;
    private final RoleBuild newBuild;
}
