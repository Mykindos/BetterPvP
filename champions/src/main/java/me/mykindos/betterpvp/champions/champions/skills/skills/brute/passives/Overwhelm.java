package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
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

@Getter
@Singleton
@BPvPListener
public class Overwhelm extends Skill implements PassiveSkill, DamageSkill {

    private double bonusDamage;

    private double healthOverTarget;

    private double maxDamage;


    @Inject
    public Overwhelm(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overwhelm";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "You deal <val>" + getBonusDamage() + "</val> bonus damage for every",
                "<val>" + getHealthOverTarget() + "</val> more health you have than your target",
                "",
                "You can deal a maximum of <val>" + getMaxDamage() + "</val> bonus damage"
        };
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
        if (hasSkill(player)) {
            LivingEntity ent = event.getDamagee();
            double difference = (player.getHealth() - ent.getHealth()) / healthOverTarget;
            if (difference > 0) {
                difference = Math.min(difference, getMaxDamage());
                event.setDamage(event.getDamage() + (difference * bonusDamage));
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        bonusDamage = getConfig("bonusDamage", 0.5, Double.class);
        healthOverTarget = getConfig("healthOverTarget", 2.0, Double.class);
        maxDamage = getConfig("maxDamage", 1.0, Double.class);
    }


}
