package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class CripplingBlow extends Skill implements PassiveSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

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
    public String[] getDescription(int level) {

        return new String[]{
                "Enemies you hit with an axe don't take knockback",
                "and receive <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength + 1) + "</effect> for <val>" + getDuration(level) + "</val> seconds"
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!SkillWeapons.isHolding(player, SkillType.AXE)) return;

        int level = getLevel(player);
        if (level > 0) {
            LivingEntity target = event.getDamagee();
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration(level) * 20), slownessStrength));
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
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        slownessStrength = getConfig("slownessStrength", 0, Integer.class);
    }

}
