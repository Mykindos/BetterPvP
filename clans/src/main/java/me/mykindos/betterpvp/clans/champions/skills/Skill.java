package me.mykindos.betterpvp.clans.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfig;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;

@Singleton
public abstract class Skill implements ISkill {

    private final SkillConfigFactory configFactory;

    @Getter
    private SkillConfig skillConfig;

    @Inject
    public Skill(SkillConfigFactory configFactory) {
        this.configFactory = configFactory;
        this.skillConfig = configFactory.create(this);
    }

    @Override
    public boolean isEnabled() {
        return skillConfig.isEnabled();
    }

    public void reload() {
        this.skillConfig = configFactory.create(this);
    }
}
