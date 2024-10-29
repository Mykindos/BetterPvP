package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class BreakFall extends Skill implements PassiveSkill, BuffSkill {

    private double baseDamageReduction;

    private double damageReductionIncreasePerLevel;

    @Inject
    public BreakFall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Break Fall";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You roll when you hit the ground",
                "",
                "Fall damage is reduced by " + getValueString(this::getDamageReduction, level),
        };
    }

    public double getDamageReduction(int level) {
        return baseDamageReduction + (damageReductionIncreasePerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onFall(CustomDamageEvent e) {
        if (!(e.getDamagee() instanceof Player player)) return;
        if (e.getCause() != DamageCause.FALL) return;

        int level = getLevel(player);
        if (level > 0) {
            if (e.getDamage() <= getDamageReduction(level)) {
                e.setCancelled(true);
            } else {
                e.setDamage(e.getDamage() - getDamageReduction(level));
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDamageReduction = getConfig("baseDamageReduction", 3.0, Double.class);
        damageReductionIncreasePerLevel = getConfig("damageReductionIncreasePerLevel", 3.0, Double.class);
    }
}
