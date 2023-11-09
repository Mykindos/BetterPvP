package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@BPvPListener
public class ShockingStrikes extends Skill implements PassiveSkill, Listener {

    private double baseDuration;

    private double durationIncrease;

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
                "<val>" + getDuration(level) + "</val> second, giving them <effect>Slowness I</effect>",
                "and <effect>Screen-Shake</effect>"
        };
    }

    public double getDuration(int level) {
        return baseDuration + durationIncrease * level;
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        int level = getLevel(damager);
        if (level <= 0) return;

        championsManager.getEffects().addEffect(damagee, EffectType.SHOCK, (long) (getDuration(level) * 1000L));
        damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (slownessDuration * 20), slownessStrength));
        event.addReason(getName());

    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("assassinDecrease", 0.0, Double.class);
        durationIncrease = getConfig("baseIncrease", 1.0, Double.class);
        slownessDuration = getConfig("slownessDuration", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 0, Integer.class);
    }
}
