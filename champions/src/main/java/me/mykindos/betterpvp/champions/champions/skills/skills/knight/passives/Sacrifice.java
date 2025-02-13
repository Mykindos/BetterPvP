package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Getter
@Singleton
@BPvPListener
public class Sacrifice extends Skill implements PassiveSkill, OffensiveSkill, DamageSkill {

    private double percentage;


    @Inject
    public Sacrifice(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sacrifice";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Deal an extra <val>" + UtilFormat.formatNumber(getPercentage() * 100, 0) + "%</val> melee damage,",
                "but you now also take <val>" + UtilFormat.formatNumber(getPercentage() * 100, 0) + "%</val> extra",
                "damage from melee attacks."
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (event.getDamager() instanceof Player damager) {
            if (hasSkill(damager)) {
                event.setDamage(event.getDamage() * (1.0 + getPercentage()));
            }

        }

        if (event.getDamagee() instanceof Player damagee) {
            if (hasSkill(damagee)) {
                event.setDamage(event.getDamage() * (1.0 + getPercentage()));
            }
        }
    }

    public void loadSkillConfig() {
        percentage = getConfig("percentage", 0.08, Double.class);
    }
}
