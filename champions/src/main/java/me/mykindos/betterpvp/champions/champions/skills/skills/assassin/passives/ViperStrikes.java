package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Singleton
@BPvPListener
public class ViperStrikes extends Skill implements PassiveSkill, Listener {

    private double baseSeconds;

    @Inject
    public ViperStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }
    @Override
    public String getDefaultClassString() {
        return "assassin";
    }
    @Override
    public String getName() {
        return "Viper Strikes";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your attacks give enemies",
                "<effect>Poison 1</effect> for <val>" + getSeconds(level) + "</val> seconds"
        };
    }

    private double getSeconds(int level) {
        return baseSeconds + (level - 1) * 2;
    }

    @Override
    public void loadSkillConfig() {
        baseSeconds = getConfig("baseSeconds", 2.0, Double.class);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        int level = getLevel(damager);
        if (level <= 0) return;

        final int ticks = (int) (getSeconds(level) * 20);
        damagee.addPotionEffect(PotionEffectType.POISON.createEffect(ticks, 0));
        event.setReason(getName());
    }

}
