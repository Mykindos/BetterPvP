package me.mykindos.betterpvp.champions.effects.types;

import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;

public class SkillBoostEffect extends EffectType {

    private final String prefix;
    private final SkillType[] skillTypes;

    public SkillBoostEffect(String prefix, SkillType... skillTypes) {
        this.prefix = prefix;
        this.skillTypes = skillTypes;
    }

    @Override
    public String getName() {
        return prefix + " Skill Boost";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    public boolean hasSkillType(SkillType skillType) {
        for (SkillType type : skillTypes) {
            if (type == skillType) {
                return true;
            }
        }
        return false;
    }
}
