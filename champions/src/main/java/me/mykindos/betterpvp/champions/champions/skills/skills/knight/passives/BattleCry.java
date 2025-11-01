package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.List;

@Singleton
@BPvPListener
public class BattleCry extends Skill implements CooldownToggleSkill, Listener, BuffSkill, CrowdControlSkill, DamageSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private int weaknessStrength;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double launchStrength;

    @Inject
    public BattleCry(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Drop your Sword/Axe to activate",
                "",
                "Unleash a fierce battle cry, inflicting",
                "<effect>Weakness " + UtilFormat.getRomanNumeral(this.weaknessStrength) + "</effect> on nearby enemies",
                "for " + getValueString(this::getDuration, level) + " seconds. Launch them",
                "and yourself upward.",
                "",
                "If grounded, slam the ground to deal",
                getValueString(this::getDamage, level) + " damage.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds",
        };
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    public double getDamage(int level) {
        return baseDamage + (level - 1) * damageIncreasePerLevel;
    }

    public double getRadius(int level) {
        return baseRadius + (level - 1) * radiusIncreasePerLevel;
    }

    @Override
    public String getName() {
        return "Battle Cry";
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public double getCooldown(int level) {
        return this.cooldown - ((level - 1) * this.cooldownDecreasePerLevel);
    }

    @Override
    public void toggle(Player player, int level) {
        long duration = (long) (getDuration(level) * 1000);
        double radius = getRadius(level);

        // Get nearby enemies
        List<LivingEntity> targets = UtilEntity.getNearbyEnemies(player, player.getLocation(), radius);

        // Apply weakness and launch enemies
        for (LivingEntity target : targets) {
            // Apply weakness effect
            championsManager.getEffects().addEffect(target, player, EffectTypes.WEAKNESS, weaknessStrength, duration);

            // Launch enemy upward
            Vector trajectory = new Vector(0, 1, 0);
            VelocityData velocityData = new VelocityData(trajectory, launchStrength, false, 0, 0.0, 1.0, true);
            UtilVelocity.velocity(target, player, velocityData);
            Particle.CLOUD.builder()
                    .count(30)
                    .location(target.getLocation().clone().add(0.0, 0.5, 0.0))
                    .receivers(60)
                    .extra(0.15)
                    .spawn();

            // Notify enemy
            if (target instanceof Player damagee) {
                UtilMessage.simpleMessage(damagee, "Skill", "<alt2>%s</alt2> hit you with <alt>%s</alt>.", player.getName(), getName());
            }
        }

        // Launch player upward
        Vector playerTrajectory = new Vector(0, 1, 0);
        VelocityData playerVelocityData = new VelocityData(playerTrajectory, launchStrength, false, 0, 0.0, 1.0, true);
        UtilVelocity.velocity(player, player, playerVelocityData);

        // Sound and particle effects
        new SoundEffect(Sound.ENTITY_RAVAGER_CELEBRATE, 1f, 1.5f).play(player);
        new SoundEffect(Sound.ENTITY_PIGLIN_JEALOUS, 0.5f, 1f).play(player);
        new SoundEffect(Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 0.5f, 1f).play(player);
        Particle.CLOUD.builder()
                .count(30)
                .location(player.getLocation().clone().add(0.0, 0.5, 0.0))
                .receivers(60)
                .extra(0.15)
                .spawn();
        Particle.ANGRY_VILLAGER.builder()
                .count(5)
                .location(player.getLocation().clone().add(0.0, 0.5, 0.0))
                .offset(radius, radius, radius)
                .receivers(60)
                .extra(0.15)
                .spawn();

        for (Location location : UtilLocation.getCircumference(player.getLocation(), radius, 40)) {
            Particle.BLOCK_CRUMBLE.builder()
                    .data(Material.ANVIL.createBlockData())
                    .location(location.add(0, 0.1, 0))
                    .count(1)
                    .receivers(60)
                    .extra(0.15)
                    .spawn();
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        weaknessStrength = getConfig("weaknessStrength", 1, Integer.class);
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseRadius = getConfig("baseRadius", 5.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
        launchStrength = getConfig("launchStrength", 1.0, Double.class);
    }
}
