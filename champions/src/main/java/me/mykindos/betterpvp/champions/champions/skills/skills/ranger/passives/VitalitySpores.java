package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class VitalitySpores extends Skill implements PassiveSkill, DefensiveSkill, HealthSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private int regenerationStrength;

    private int regenerationStrengthIncreasePerLevel;

    @Inject
    public VitalitySpores(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Vitality Spores";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Every time you hit an arrow, forest",
                "spores surround you, giving you",
                "<effect>Regeneration " + UtilFormat.getRomanNumeral(getRegenerationStrength(level)) + "</effect> for " + getValueString(this::getDuration, level) + " seconds"
        };
    }

    public double getDuration(int level) {
        return baseDuration - ((level - 1) * durationIncreasePerLevel);
    }

    public int getRegenerationStrength(int level){
        return regenerationStrength + ((level - 1) * regenerationStrengthIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        int level = getLevel(player);
        if (level > 0) {
            championsManager.getEffects().addEffect(player, EffectTypes.REGENERATION, getName(), getRegenerationStrength(level), (long) (getDuration(level) * 1000));
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.5, Double.class);
        durationIncreasePerLevel = getConfig("durationDecreasePerLevel", 0.0, Double.class);
        regenerationStrength = getConfig("regenerationStrength", 2, Integer.class);
        regenerationStrengthIncreasePerLevel = getConfig("regenerationStrengthIncreasePerLevel", 1, Integer.class);
    }
}
