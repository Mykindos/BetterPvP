package me.mykindos.betterpvp.clans.champions.skills.types;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class PrepareSkill extends Skill implements InteractSkill {

    protected final Set<UUID> active = new HashSet<>();
    public PrepareSkill(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public boolean canUse(Player player) {
        return !active.contains(player.getUniqueId());
    }
}
