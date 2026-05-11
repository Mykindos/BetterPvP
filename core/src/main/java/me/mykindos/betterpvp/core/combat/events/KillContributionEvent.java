package me.mykindos.betterpvp.core.combat.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
@EqualsAndHashCode(callSuper = true)
public class KillContributionEvent extends CustomEvent {

    private final Long killId;
    private final Player victim;
    private final Player killer;
    private final Map<Player, Contribution> contributions;
    private final CompletableFuture<Void> savePromise;

    public KillContributionEvent(Long killId, Player victim, Player killer, Map<Player, Contribution> contributions) {
        this.killId = killId;
        this.victim = victim;
        this.killer = killer;
        this.contributions = contributions;
        this.savePromise = new CompletableFuture<>();
    }

}
