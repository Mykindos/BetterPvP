package me.mykindos.betterpvp.champions.stats;

import lombok.Getter;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.Kill;
import me.mykindos.betterpvp.core.components.champions.Role;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class ChampionsKill extends Kill {

    private final Role killerRole;
    private final Role victimRole;
    private final Map<Contribution, Role> contributionRoles;

    public ChampionsKill(UUID killer, UUID victim, int ratingDelta, List<Contribution> contributions, Role killerRole, Role victimRole, Map<Contribution, Role> contributionRoles) {
        super(killer, victim, ratingDelta, contributions);
        this.killerRole = killerRole;
        this.victimRole = victimRole;
        this.contributionRoles = contributionRoles;
    }
}
