package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

@Singleton
@BPvPListener
public class CripplingBlow extends Skill implements PassiveSkill, DebuffSkill {

    @Getter
    private double duration;
    private int slownessStrength;

    @Inject
    public CripplingBlow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Crippling Blow";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Enemies you hit with an axe don't take knockback",
                "and receive <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> for <val>" + getDuration() + "</val> seconds"
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!SkillWeapons.isHolding(player, SkillType.AXE)) return;

        if (hasSkill(player)) {
            LivingEntity target = event.getDamagee();
            championsManager.getEffects().addEffect(target, player, EffectTypes.SLOWNESS, slownessStrength, (long) (getDuration() * 1000));
            event.addReason(getName());
            event.setKnockback(false);
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 2.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
    }

}
