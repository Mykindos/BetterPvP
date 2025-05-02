package me.mykindos.betterpvp.champions.champions.skills.events;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

/**
 * This event is triggered when a player successfully uses a crowd control skill.
 * It is used to notify {@link me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives.ThreateningShout}
 * about the successful use of a crowd control skill.
 */
@Getter
public class SuccessfulCrowdControlSkillUseEvent extends CustomEvent {
    private final Player player;

    public SuccessfulCrowdControlSkillUseEvent(Player player) {
        this.player = player;
    }
}
