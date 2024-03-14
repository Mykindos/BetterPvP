package me.mykindos.betterpvp.champions.effects;

import me.mykindos.betterpvp.champions.effects.types.SkillBoostEffect;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;

public class ChampionsEffectTypes {

    public static final EffectType SWORD_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Sword", SkillType.SWORD));
    public static final EffectType AXE_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Axe", SkillType.AXE));
    public static final EffectType BOW_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Bow", SkillType.BOW));
    public static final EffectType PASSIVE_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Passive", SkillType.PASSIVE_A, SkillType.PASSIVE_B, SkillType.GLOBAL));

}
