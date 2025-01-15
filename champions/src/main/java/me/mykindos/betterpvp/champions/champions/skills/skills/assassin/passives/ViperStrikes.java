package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class ViperStrikes extends Skill implements PassiveSkill, Listener, DebuffSkill, OffensiveSkill {

    @Getter
    private double duration;
    private int poisonStrength;

    @Inject
    public ViperStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Viper Strikes";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Your attacks give enemies",
                "<effect>Poison " + UtilFormat.getRomanNumeral(poisonStrength) + "</effect> for <val>" + getDuration() + "</val> seconds",
                "",
                EffectTypes.POISON.getDescription(poisonStrength)
        };
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 2.0, Double.class);
        poisonStrength = getConfig("poisonStrength", 1, Integer.class);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasSkill(damager)) return;

        championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.POISON, poisonStrength, (long) (getDuration() * 1000L));
        event.getDamagee().getWorld().playSound(damager.getLocation(), Sound.ENTITY_SPIDER_HURT, 1f, 2f);
        event.addReason(getName());
    }

}
