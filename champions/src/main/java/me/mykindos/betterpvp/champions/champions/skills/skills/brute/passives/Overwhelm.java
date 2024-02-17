package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Overwhelm extends Skill implements PassiveSkill {

    private double bonusDamage;

    private double healthOverTarget;

    private double baseMaxDamage;

    private double maxDamageIncreasePerLevel;

    @Inject
    public Overwhelm(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overwhelm";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You deal <stat>" + bonusDamage + "</stat> bonus damage for every",
                "<stat>" + healthOverTarget + "</stat> more health you have than your target",
                "",
                "You can deal a maximum of <val>" + String.format("%.1f", getMaxDamage(level)) + "</val> bonus damage"
        };
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + ((level-1) * maxDamageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        int level = getLevel(player);
        if (level > 0) {
            LivingEntity ent = event.getDamagee();
            double difference = (player.getHealth() - ent.getHealth()) / healthOverTarget;
            if (difference > 0) {
                difference = Math.min(difference, getMaxDamage(level));
                event.setDamage(event.getDamage() + difference * bonusDamage);
            }
        }
    }

    @Override
    public void loadSkillConfig(){
        bonusDamage = getConfig("bonusDamage", 1.0, Double.class);
        healthOverTarget = getConfig("healthOverTarget", 2.0, Double.class);
        baseMaxDamage = getConfig("baseMaxDamage", 0.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 0.5, Double.class);
    }


}
