package me.mykindos.betterpvp.champions.effects;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.effects.types.SkillBoostEffect;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import org.bukkit.plugin.java.JavaPlugin;

public class ChampionsEffectTypes {

    public static final EffectType SWORD_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Sword", SkillType.SWORD), JavaPlugin.getPlugin(Champions.class));
    public static final EffectType AXE_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Axe", SkillType.AXE), JavaPlugin.getPlugin(Champions.class));
    public static final EffectType BOW_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Bow", SkillType.BOW), JavaPlugin.getPlugin(Champions.class));
    public static final EffectType PASSIVE_SKILL_BOOST = EffectTypes.createEffectType(new SkillBoostEffect("Passive", SkillType.PASSIVE_A, SkillType.PASSIVE_B, SkillType.GLOBAL), JavaPlugin.getPlugin(Champions.class));

}
