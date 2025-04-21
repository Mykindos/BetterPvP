package me.mykindos.betterpvp.champions.stats.repository.skill;

import java.util.Map;
import lombok.Data;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.stats.ChampionsKill;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;

@Data
public class ChampionsKillSkillData {
    private final ChampionsKill kill;
    private final RoleBuild killerBuild;
    private final RoleBuild victimBuild;
    private final Map<Contribution, RoleBuild> contributorBuild;
}
