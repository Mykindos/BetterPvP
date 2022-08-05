package me.mykindos.betterpvp.clans.champions.skills.types;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class ChannelSkill extends Skill {

    protected final Set<UUID> active = new HashSet<>();
    public ChannelSkill(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }
}
