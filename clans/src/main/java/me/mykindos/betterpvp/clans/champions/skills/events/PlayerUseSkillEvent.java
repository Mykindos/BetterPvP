package me.mykindos.betterpvp.clans.champions.skills.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerUseSkillEvent extends CustomCancellableEvent {

    private final Player player;
    private final Skill skill;
    private final int level;

}