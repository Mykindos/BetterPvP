package me.mykindos.betterpvp.clans.champions.skills.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillConfig {

    private final int cooldown;
    private final int energyCost;
    private final boolean enabled;

}
