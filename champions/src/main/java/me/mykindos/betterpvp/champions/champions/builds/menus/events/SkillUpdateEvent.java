package me.mykindos.betterpvp.champions.champions.builds.menus.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class SkillUpdateEvent extends CustomEvent {

    private final Player player;
    private final BuildSkill buildSkill;
    private final RoleBuild roleBuild;
    private final RoleBuild previous;

}
