package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
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
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class BarbedArrows extends Skill implements PassiveSkill, DebuffSkill, OffensiveSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int slownessStrength;

    @Inject
    public BarbedArrows(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Barbed Arrows";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Your arrows apply <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> to any",
                "damageable target for " + getValueString(this::getDuration, level) + " seconds"
        };
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSlow(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level > 0) {
            event.addReason(getName());
            championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SLOWNESS, slownessStrength, (long) (getDuration(level) * 1000));
        }
    }

    @Override
    public void loadSkillConfig(){
        baseDuration = getConfig("baseDuration", 2.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        slownessStrength = getConfig("slownessStrength", 2, Integer.class);
    }

}
