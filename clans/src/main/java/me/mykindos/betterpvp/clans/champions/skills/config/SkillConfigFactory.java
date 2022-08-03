package me.mykindos.betterpvp.clans.champions.skills.config;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.skills.Skill;


public class SkillConfigFactory implements ISkillConfigFactory {

    private final Clans clans;

    @Inject
    public SkillConfigFactory(Clans clans){
        this.clans = clans;
    }

    @Override
    public SkillConfig create(Skill skill) {
        var config = clans.getConfig();

        String path = "skills." + skill.getClassType().toString().toLowerCase() + "." + skill.getName().toLowerCase();

        boolean enabled = config.getOrSaveBoolean(path + ".enabled", true);
        int cooldown = config.getOrSaveInt(path + ".cooldown", 0);
        int energy = config.getOrSaveInt(path + ".energy", 0);

        return SkillConfig.builder()
                .enabled(enabled)
                .cooldown(cooldown)
                .energyCost(energy)
                .build();
    }
}
