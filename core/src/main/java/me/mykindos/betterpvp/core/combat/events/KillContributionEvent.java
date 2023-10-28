package me.mykindos.betterpvp.core.combat.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class KillContributionEvent extends CustomEvent {

    private final UUID killId = UUID.randomUUID();
    private final Player victim;
    private final Player killer;
    private final Map<Player, Contribution> contributions;

}
