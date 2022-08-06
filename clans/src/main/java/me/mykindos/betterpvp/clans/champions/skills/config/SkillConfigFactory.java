package me.mykindos.betterpvp.clans.champions.skills.config;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;


public class SkillConfigFactory implements ISkillConfigFactory {

    private final Clans clans;

    @Inject
    public SkillConfigFactory(Clans clans) {
        this.clans = clans;
    }

    @Override
    public SkillConfig create(Skill skill) {
        var config = clans.getConfig();

        String path = "skills." + skill.getClassType().toString().toLowerCase() + "." + skill.getName().replace(" ", "").toLowerCase();

        boolean enabled = config.getOrSaveBoolean(path + ".enabled", true);
        int maxLevel = config.getOrSaveInt(path + ".maxlevel", 5);
        int cooldown = 0;
        int energy = 0;
        if (skill instanceof CooldownSkill) {
            cooldown = config.getOrSaveInt(path + ".cooldown", 0);

        }
        if (skill instanceof EnergySkill) {
            energy = config.getOrSaveInt(path + ".energy", 0);
        }
        return SkillConfig.builder()
                .enabled(enabled)
                .cooldown(cooldown)
                .energyCost(energy)
                .maxlevel(maxLevel)
                .build();
    }
}
