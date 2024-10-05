package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
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

@BPvPListener
@Singleton
public class MagmaBlade extends Skill implements PassiveSkill, FireSkill, DamageSkill {

    private double baseDamage;

    private double damageIncreasePerLevel;

    @Inject
    public MagmaBlade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Magma Blade";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Your sword is fueled by flames,",
                "dealing an additional " + getValueString(this::getDamage, level) + " damage",
                "to players who are on fire but",
                "also extinguishes them"
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level-1) * damageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!SkillWeapons.isHolding(player, SkillType.SWORD)) return;

        int level = getLevel(player);
        if (level > 0) {
            LivingEntity ent = event.getDamagee();
            if (ent.getFireTicks() > 0) {
                event.setDamage(event.getDamage() + getDamage(level));
                ent.setFireTicks(0);
            }
        }

    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
    }
}