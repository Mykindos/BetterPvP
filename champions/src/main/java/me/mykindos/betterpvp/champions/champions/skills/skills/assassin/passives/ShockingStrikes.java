package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class ShockingStrikes extends Skill implements PassiveSkill, Listener, DebuffSkill, OffensiveSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private double slownessDuration;

    private int slownessStrength;

    @Inject
    public ShockingStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Shocking Strikes";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your attacks <effect>Shock</effect> targets for",
                getValueString(this::getDuration, level) + " seconds, giving them <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect>",
                "and <effect>Screen-Shake</effect>",
                "",
                EffectTypes.SHOCK.getDescription(0)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        int level = getLevel(damager);
        if (level <= 0) return;

        championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SHOCK, (long) (getDuration(level) * 1000L));
        championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SLOWNESS, slownessStrength, (long) (slownessDuration * 1000));
        event.addReason(getName());

    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        slownessDuration = getConfig("slownessDuration", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
    }
}
