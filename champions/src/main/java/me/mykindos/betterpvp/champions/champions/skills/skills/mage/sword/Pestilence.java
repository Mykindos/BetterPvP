package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.PestilenceData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
@BPvPListener
public class Pestilence extends Skill implements CooldownSkill, Listener, InteractSkill, OffensiveSkill, DebuffSkill {

    private final Map<LivingEntity, PestilenceData> pestilenceDataMap = new ConcurrentHashMap<>();
    private final Champions champions;
    private double infectionDuration;
    private double infectionDurationIncreasePerLevel;
    private double enemyDamageReduction;
    private double enemyDamageReductionIncreasePerLevel;
    private double radius;
    private double radiusIncreasePerLevel;
    private double cloudDuration;
    private double cloudDurationIncreasePerLevel;

    @Inject
    public Pestilence(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        this.champions = champions;
    }

    @Override
    public String getName() {
        return "Pestilence";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Shoot out a poison cloud that will travel for " + getValueString(this::getCloudDuration, level) + " seconds, infecting",
                "anyone it hits and spreading to nearby enemies within " + getValueString(this::getRadius, level) + " blocks",
                "",
                "While enemies are infected, they receive <effect>Poison</effect> and",
                "deal " + getValueString(this::getEnemyDamageReduction, level, 100, "%", 0) + " less damage",
                "",
                "<effect>Pestilence</effect> lasts " + getValueString(this::getInfectionDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getEnemyDamageReduction(int level) {
        return enemyDamageReduction + ((level - 1) * enemyDamageReductionIncreasePerLevel);
    }

    public double getInfectionDuration(int level) {
        return infectionDuration + ((level - 1) * infectionDurationIncreasePerLevel);
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    public double getCloudDuration(int level) {
        return cloudDuration + ((level - 1) * cloudDurationIncreasePerLevel);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void activate(Player player, int level) {
        createPoisonCloud(player, level);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 1.0f, 2.0f);

    }

    private void createPoisonCloud(Player caster, int level) {
        new BukkitRunnable() {
            final Location originalLocation = caster.getLocation().add(0, 1, 0);
            final Location currentLocation = originalLocation;
            final long endTime = System.currentTimeMillis() + (long) (getCloudDuration(level) * 1000);

            @Override
            public void run() {
                if (System.currentTimeMillis() > endTime) {
                    this.cancel();
                    return;
                }

                spawnParticles(currentLocation, 10);
                for (LivingEntity entity : UtilEntity.getNearbyEnemies(caster, currentLocation, 2.0)) {
                    if (!pestilenceDataMap.containsKey(entity)) {
                        infectEntity(caster, entity, level, caster);
                    }
                }
                if(!UtilBlock.airFoliage(currentLocation.add(originalLocation.getDirection().normalize().multiply(0.5)).getBlock())) {
                    this.cancel();
                }
                currentLocation.add(originalLocation.getDirection().normalize().multiply(0.25));
            }
        }.runTaskTimer(champions, 1L, 1L);
    }

    private void infectEntity(LivingEntity caster, LivingEntity target, int level, Player originalCaster) {
        if (originalCaster == null) return;

        pestilenceDataMap.computeIfAbsent(caster, k -> new PestilenceData()).addInfected(target);
        PestilenceData data = pestilenceDataMap.get(caster);
        data.getInfectionTimers().put(target, System.currentTimeMillis() + (long) (getInfectionDuration(level) * 1000));
        championsManager.getEffects().addEffect(target, EffectTypes.POISON, 1, (long) (getInfectionDuration(level) * 1000));
        data.getOriginalCasters().put(target, originalCaster);
        for (LivingEntity ent : UtilEntity.getNearbyEnemies(originalCaster, target.getLocation(), getRadius(level))) {
            if(ent.equals(caster)) return;
            System.out.println(pestilenceDataMap.get(caster).getSentTrackingTrail());
            if (!pestilenceDataMap.get(originalCaster).getSentTrackingTrail().getOrDefault(ent, false)) {
                pestilenceDataMap.get(originalCaster).getSentTrackingTrail().put(ent, true);
                createTrackingTrail(target, ent, originalCaster);
            }
        }
    }

    @UpdateEvent
    public void removePoison() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<LivingEntity, PestilenceData>> iterator = pestilenceDataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, PestilenceData> entry = iterator.next();
            PestilenceData data = entry.getValue();

            Iterator<Map.Entry<LivingEntity, Long>> infectionIterator = data.getInfectionTimers().entrySet().iterator();
            while (infectionIterator.hasNext()) {
                Map.Entry<LivingEntity, Long> infectionEntry = infectionIterator.next();
                LivingEntity infected = infectionEntry.getKey();
                long endTime = infectionEntry.getValue();

                if (currentTime > endTime || infected == null || infected.isDead()) {
                    infectionIterator.remove();
                    data.removeInfected(infected);
                    data.getTrackingTasks().remove(infected);
                    data.getOriginalCasters().remove(infected);
                    data.getInfectionTimers().remove(infected);
                    data.getSentTrackingTrail().remove(infected);
                }
            }

            if (data.getInfectedTargets().isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void createTrackingTrail(LivingEntity source, LivingEntity target, Player originalCaster) {
        if (originalCaster == null) return;

        long startTime = System.currentTimeMillis();
        double trailDuration = getCloudDuration(getLevel(originalCaster)) * 1000;
        BukkitRunnable trailTask = new BukkitRunnable() {
            final Location currentLocation = source.getEyeLocation();

            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (target.isDead() || elapsedTime > trailDuration) {
                    this.cancel();
                    return;
                }

                Vector direction = target.getEyeLocation().subtract(currentLocation).toVector().normalize().multiply(0.25);

                if (!UtilBlock.airFoliage(currentLocation.add(direction).getBlock())) {
                    this.cancel();
                    return;
                }

                currentLocation.add(direction);
                spawnParticles(currentLocation, 1);

                if (currentLocation.distance(target.getEyeLocation()) <= 0.5) {
                    infectEntity(source, target, getLevel(originalCaster), originalCaster);
                    this.cancel();
                }
            }
        };

        trailTask.runTaskTimer(champions, 1L, 1L);
        pestilenceDataMap.computeIfAbsent(source, k -> new PestilenceData()).getTrackingTasks().put(source, trailTask);

    }

    private void spawnParticles(Location location, int count) {
        Particle.DustOptions poisonDust = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1);
        new ParticleBuilder(Particle.DUST)
                .location(location)
                .count(count)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .data(poisonDust)
                .receivers(60)
                .spawn();

        Random random = UtilMath.RANDOM;
        double dx = (random.nextDouble() - 0.5) * 0.2;
        double dy = (random.nextDouble() - 0.5) * 0.2;
        double dz = (random.nextDouble() - 0.5) * 0.2;

        Location particleLocation = location.clone().add(dx, dy, dz);

        double red = 0.4;
        double green = 0.8;
        double blue = 0.4;
        Color color = Color.fromRGB((int) (red * 255), (int) (green * 255), (int) (blue * 255));

        new ParticleBuilder(Particle.ENTITY_EFFECT)
                .location(particleLocation)
                .count(1)
                .data(color)
                .receivers(60)
                .spawn();
    }


    @EventHandler
    public void onDamageReduction(CustomDamageEvent event) {
        if (event.getDamager() == null) return;
        if (!(pestilenceDataMap.containsKey(event.getDamager()))) return;

        Player caster = pestilenceDataMap.get(event.getDamager()).getOriginalCasters().get(event.getDamager());

        double reduction = getEnemyDamageReduction(getLevel(caster));
        event.setDamage(event.getDamage() * (1 - reduction));
    }

    @UpdateEvent(delay = 1000)
    public void displayPestilence() {
        pestilenceDataMap.forEach((caster, data) -> {
            data.getInfectedTargets().forEach(infected -> {
                for (int q = 0; q <= 10; q++) {
                    final float x = (float) (1 * Math.cos(2 * Math.PI * q / 10));
                    final float z = (float) (1 * Math.sin(2 * Math.PI * q / 10));

                    Bukkit.getScheduler().scheduleSyncDelayedTask(champions, () -> {
                        if (data.getInfectionTimers().containsKey(infected)) {
                            new ParticleBuilder(Particle.HAPPY_VILLAGER)
                                    .location(infected.getLocation().add(x, 1, z))
                                    .receivers(30)
                                    .extra(0)
                                    .spawn();
                        }
                    }, q * 5L);
                }
            });
        });
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        infectionDuration = getConfig("infectionDuration", 5.0, Double.class);
        infectionDurationIncreasePerLevel = getConfig("infectionDurationIncreasePerLevel", 0.0, Double.class);
        enemyDamageReduction = getConfig("enemyDamageReduction", 0.20, Double.class);
        enemyDamageReductionIncreasePerLevel = getConfig("enemyDamageReductionIncreasePerLevel", 0.0, Double.class);
        radius = getConfig("radius", 5.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 2.0, Double.class);
        cloudDuration = getConfig("cloudDuration", 4.0, Double.class);
        cloudDurationIncreasePerLevel = getConfig("cloudDurationIncreasePerLevel", 1.0, Double.class);
    }
}