package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

@Singleton
@BPvPListener
public class Entangle extends Skill implements PassiveSkill {

    private double baseDuration;

    @Inject
    public Entangle(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Entangle";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Your arrows apply <effect>Slowness II</effect> to any",
                "damageable target for <val>" + (baseDuration + (level * 0.5)) + "</val> seconds"
        };
    }

    @Override
    public String getDefaultClassString() {
        return "ranger";
    }
    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSlow(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level > 0) {
            event.setReason(getName());
            event.getDamagee().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((baseDuration + (level * 0.5)) * 20), 1));
        }
    }

    @Override
    public void loadSkillConfig(){
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
    }

}
