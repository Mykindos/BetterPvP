package me.mykindos.betterpvp.core.combat.events;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

import java.util.Map;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KillContributionEvent extends CustomEvent {

    private final Long killId;
    private final Player victim;
    private final Player killer;
    private final Map<Player, Contribution> contributions;

}
