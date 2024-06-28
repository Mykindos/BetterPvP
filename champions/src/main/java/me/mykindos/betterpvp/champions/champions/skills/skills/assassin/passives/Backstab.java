package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@BPvPListener
public class Backstab extends Skill implements PassiveSkill, Listener, DamageSkill, OffensiveSkill {

    private double damageIncreasePerLevel;
    private double damage;

    @Inject
    public Backstab(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Backstab";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hitting an enemy from behind will",
                "increase your damage by " + getValueString(this::getDamageModifier, level, 1, "", 1),
        };
    }

    public double getDamageModifier(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    @EventHandler
    public void onEntDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level <= 0) return;

        if (UtilMath.getAngle(damager.getLocation().getDirection(), event.getDamagee().getLocation().getDirection()) < 60) {
            event.setDamage(event.getDamage() + getDamageModifier(level));
            damager.getWorld().playSound(event.getDamagee().getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT, 1f, 2f);
            damager.getWorld().playEffect(event.getDamagee().getLocation().add(0, 1, 0), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            event.addReason("Backstab");
        }
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        damageIncreasePerLevel = getConfig("increasePerLevel", 1.5, Double.class);
        damage = getConfig("baseDamage", 1.5, Double.class);
    }
}
