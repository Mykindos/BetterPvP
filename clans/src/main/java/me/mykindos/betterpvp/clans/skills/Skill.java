package me.mykindos.betterpvp.clans.skills;

import com.google.inject.Inject;
import lombok.Getter;
import me.mykindos.betterpvp.clans.skills.config.SkillConfig;
import me.mykindos.betterpvp.clans.skills.config.SkillConfigFactory;

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
