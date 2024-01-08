package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@BPvPListener
public class Backstab extends Skill implements PassiveSkill, Listener {

    private double baseIncrease;
    private double increasePerLevel;
    private double assassinDecrease;

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
                "Hitting an enemy from behind with a sword will",
                "increase your damage by <val>" + Math.floor((getDamageModifier(level) - 1) * 100) + "%"};
    }

    public double getDamageModifier(int level) {
        return 1 + (baseIncrease + (level * increasePerLevel));
    }

    @EventHandler
    public void onEntDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!SkillWeapons.isHolding(damager, SkillType.SWORD)) return;

        int level = getLevel(damager);
        if (level <= 0) return;

        if (UtilMath.getAngle(damager.getLocation().getDirection(), event.getDamagee().getLocation().getDirection()) < 60) {
            event.setDamage(event.getDamage() * getDamageModifier(level));
            damager.getWorld().playEffect(event.getDamagee().getLocation().add(0, 1, 0), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);

            if (event.getDamagee() instanceof Player damagee) {
                Optional<Role> roleOptional = championsManager.getRoles().getObject(damagee.getUniqueId());
                roleOptional.ifPresent(role -> {
                    if (role == Role.ASSASSIN) {
                        event.setDamage(event.getDamage() * (1.0 - assassinDecrease));
                    }
                });
            }
            event.addReason("Backstab");
        }
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig(){
        increasePerLevel = getConfig("increasePerLevel", 0.1, Double.class);
        assassinDecrease = getConfig("assassinDecrease", 0.2, Double.class);
        baseIncrease = getConfig("baseIncrease", 0.2, Double.class);
    }
}
