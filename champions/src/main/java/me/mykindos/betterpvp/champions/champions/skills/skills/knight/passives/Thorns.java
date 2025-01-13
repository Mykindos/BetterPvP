package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Thorns extends Skill implements PassiveSkill, Listener, DefensiveSkill, DamageSkill {

    private final WeakHashMap<LivingEntity, Long> cd = new WeakHashMap<>();

    @Getter
    private double internalCooldown;

    @Getter
    private double damage;

    @Inject
    public Thorns(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Thorns";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Enemies take <val>" + getDamage() + "</val> damage when",
                "they hit you using a melee attack",
                "",
                "Internal Cooldown: <val>" + getInternalCooldown(),
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player p)) return;
        if (event.getDamager() == null) return;

        if (hasSkill(p)) {
            LivingEntity damager = event.getDamager();
            if (!cd.containsKey(damager)) {
                cd.put(damager, System.currentTimeMillis());
            } else {
                if (UtilTime.elapsed(cd.get(damager), (long) (internalCooldown * 1000L))) {
                    UtilDamage.doCustomDamage(new CustomDamageEvent(damager, p, null, DamageCause.CUSTOM, getDamage(), false, getName()));
                    cd.put(damager, System.currentTimeMillis());
                }
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        internalCooldown = getConfig("internalCooldown", 2.0, Double.class);
        damage = getConfig("damage", 0.8, Double.class);
    }
}
