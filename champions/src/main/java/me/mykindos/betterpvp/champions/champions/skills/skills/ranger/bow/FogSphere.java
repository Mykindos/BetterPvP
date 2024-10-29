package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Transformation;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;


@Singleton
@BPvPListener
public class FogSphere extends PrepareArrowSkill implements AreaOfEffectSkill, DebuffSkill, OffensiveSkill {

    private final WeakHashMap<Player, StormData> activeSpheres = new WeakHashMap<>();

    private double radius;
    private double duration;
    private double increaseDurationPerLevel;
    private double radiusIncreasePerLevel;
    private double burstDuration;

    @Inject
    public FogSphere(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fog Sphere";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that creates a sphere around the impact point,",
                "which <effect>Slows</effect> and <effect>Blinds</effect> all enemies",
                "within a " + getValueString(this::getRadius, level) + " block radius in bursts.",
                "",
                "The effect lasts for " + getValueString(this::getDuration, level) + " seconds,",
                "applying these effects every " + getValueString(this::getBurstDuration, level) + " seconds while enemies are inside the radius.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getRadius(int level) {
        return radius + (level - 1) * radiusIncreasePerLevel;
    }

    public double getDuration(int level) {
        return duration + (level - 1) * increaseDurationPerLevel;
    }
    public double getBurstDuration(int level){
        return burstDuration;
    }


    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<Player, StormData>> it = activeSpheres.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, StormData> entry = it.next();
            Player player = entry.getKey();

            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            int level = getLevel(player);

            if (level <= 0) {
                it.remove();
                continue;
            }

            if (UtilTime.elapsed(entry.getValue().getTimestamp(), (long) getDuration(level) * 1000L)) {
                it.remove();
                continue;
            }

            Location location = entry.getValue().getLocation();
            //Spawn particles every second and silence in bursts
            if (UtilTime.elapsed(entry.getValue().getLastBurst(), 1000L)) {
                entry.getValue().setLastBurst(System.currentTimeMillis());

                for (Location point : UtilLocation.getSphere(entry.getValue().getLocation(), radius, 25)) {
                    spawnParticles(player, point);
                }

                player.getWorld().playSound(entry.getValue().getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5F, 2.0F);

                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, location, radius)) {
                    if (target.hasLineOfSight(location)){
                        championsManager.getEffects().addEffect(target, player, EffectTypes.BLINDNESS, (long) burstDuration * 1000L);
                        championsManager.getEffects().addEffect(target, EffectTypes.SLOWNESS, (long) burstDuration * 1000L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.contains(arrow)) return;
        if (!hasSkill(player)) return;


        for (Location point : UtilLocation.getSphere(arrow.getLocation(), radius, 25)) {
            spawnParticles(player, point);
        }

        player.getWorld().playSound(arrow.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5F, 2.0F);

        activeSpheres.put(player, new StormData(System.currentTimeMillis(), System.currentTimeMillis(), arrow.getLocation()));

        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, arrow.getLocation(), radius)) {
            if (target.hasLineOfSight(arrow.getLocation())){
                championsManager.getEffects().addEffect(target, player, EffectTypes.BLINDNESS, (long) burstDuration * 1000L);
                championsManager.getEffects().addEffect(target, EffectTypes.SLOWNESS, (long) burstDuration * 1000L);
            }
        }
    }

    private void spawnParticles(Player player, Location point) {
        final Color color = UtilMath.randDouble(0.0, 1.0) > 0.5 ? Color.BLACK : Color.GRAY;
        final Color toColor = UtilMath.randDouble(0.0, 1.0) > 0.5 ? Color.GRAY : Color.SILVER;
        new ParticleBuilder(Particle.DUST_COLOR_TRANSITION)
                .location(point)
                .count(1)
                .extra(1)
                .data(new Particle.DustTransition(color, toColor, 1.5f))
                .source(player)
                .receivers(60)
                .spawn();
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.SMOKE)
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig(){
        radius = getConfig("radius", 5.0, Double.class);
        duration = getConfig("duration", 4.0, Double.class);
        increaseDurationPerLevel = getConfig("increaseDurationPerLevel", 0.5, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.0, Double.class);
        burstDuration = getConfig("burstDuration", 1.0, Double.class);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private class StormData {
        private final long timestamp;
        private long lastBurst;
        private final Location location;
    }
}
