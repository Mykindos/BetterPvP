package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Sacrifice extends Skill implements PassiveSkill {

    @Inject
    public Sacrifice(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sacrifice";
    }

    @Override
    public String[] getDescription(int level) {

        double percentage = ((level * 0.08) * 100);
        return new String[]{
                "Deal an extra <val>" + percentage + "%" + "</val> damage,",
                "but you now also take <val>" + percentage + "%",
                "extra damage from melee attacks"
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
            int level = getLevel(damager);
            if (level > 0) {
                event.setDamage(Math.ceil(event.getDamage() * (1.0 + (level * 0.08))));
            }

        }

        if (event.getDamagee() instanceof Player damagee) {
            int level = getLevel(damagee);
            if (level > 0) {
                event.setDamage(Math.ceil(event.getDamage() * (1.0 + (level * 0.08))));
            }
        }
    }

}

