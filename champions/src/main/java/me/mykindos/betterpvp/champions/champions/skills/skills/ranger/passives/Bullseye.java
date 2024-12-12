package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Bullseye extends Skill implements CooldownToggleSkill, DamageSkill, OffensiveSkill, TeamSkill, Listener {
    private final WeakHashMap<UUID, BullseyeData> bullsEyeData = new WeakHashMap<>();
    private double baseCurveDistance;
    private double enemyCurveDistanceIncreasePerLevel;
    private double friendlyCurveDistanceIncreasePerLevel;
    private double baseBonusDamage;
    private double baseBonusDamageIncreasePerLevel;
    private double hitboxSize;
    private double expireTime;
    private double effectiveDistance;
    private double effectiveDistanceIncreasePerLevel;

    @Inject
    public Bullseye(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bullseye";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Drop Sword / Axe to activate",
                "",
                "Look at an enemy within " + getValueString(this::getEffectiveDistance, level) + " blocks to mark",
                "them as the target. When you next shoot an arrow",
                "towards that enemy it will curve towards them",
                "from a distance of up to " + getValueString(this::getEnemyCurveDistance, level) + " blocks and also deal",
                getValueString(this::getBonusDamage, level) + " bonus damage",
                "",
                "Arrows shot at allies curve for up to " + getValueString(this::getFriendlyCurveDistance, level) + " blocks",
                "",
                "Targets expire after " + getValueString(this::getTargetExpireTime, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public double getEnemyCurveDistance(int level) {
        return baseCurveDistance + ((level - 1) * enemyCurveDistanceIncreasePerLevel);
    }

    public double getFriendlyCurveDistance(int level) {
        return baseCurveDistance + ((level - 1) * friendlyCurveDistanceIncreasePerLevel);
    }

    public double getBonusDamage(int level) {
        return baseBonusDamage + ((level - 1) * baseBonusDamageIncreasePerLevel);
    }

    public double getTargetExpireTime(int level) {
        return expireTime;
    }

    public double getEffectiveDistance(int level) {
        return effectiveDistance + ((level - 1) * effectiveDistanceIncreasePerLevel);
    }

    @Override
    public void toggle(Player player, int level) {
        UUID playerUUID = player.getUniqueId();

        LivingEntity potentialTarget = getPotentialTarget(player);
        if (potentialTarget == null) {
            UtilMessage.simpleMessage(player, "Ranger", "Bullseye failed.");
            return;
        }

        long expirationTimeInMilliseconds = (long) (getTargetExpireTime(level) * 1000L);
        long expirationDate = System.currentTimeMillis() + expirationTimeInMilliseconds;
        boolean isFriendly = UtilEntity.isEntityFriendly(player, potentialTarget);

        BullseyeData dataToPut = new BullseyeData(expirationDate, potentialTarget, null, isFriendly);
        bullsEyeData.put(playerUUID, dataToPut);
        spawnFocusingParticles(player, bullsEyeData.get(playerUUID).getTarget());
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        UUID playerUUID = player.getUniqueId();
        BullseyeData data = bullsEyeData.get(playerUUID);
        if (data == null) return;

        LivingEntity target = data.getTarget();
        if (target == null) return;

        Entity projectile = event.getProjectile();
        if (!(projectile instanceof Arrow arrow)) return;

        attemptToCurveArrow(player, arrow, playerUUID, projectile);
    }

    private void attemptToCurveArrow(Player player, Arrow arrow, UUID playerUUID, Entity projectile) {
        bullsEyeData.get(player.getUniqueId()).setArrow(arrow);

        new BukkitRunnable() {
            @Override
            public void run() {
                BullseyeData data = bullsEyeData.get(playerUUID);

                if (data == null) {
                    this.cancel();
                    bullsEyeData.remove(playerUUID);
                    return;
                }

                int level = getLevel(player);
                double radius = (data.isFriendly()) ? getFriendlyCurveDistance(level) : getEnemyCurveDistance(level);
                Collection<LivingEntity> nearbyEntities = arrow.getLocation().getNearbyLivingEntities(radius);

                LivingEntity target = data.getTarget();
                if (!arrow.isValid() || target == null || !target.isValid()) {
                    this.cancel();
                    bullsEyeData.remove(playerUUID);
                    return;
                }

                if (nearbyEntities.contains(target)) {

                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1);
                    new ParticleBuilder(Particle.DUST)
                            .location(arrow.getLocation())
                            .count(1)
                            .offset(0.1, 0.1, 0.1)
                            .extra(0)
                            .receivers(60)
                            .data(dustOptions)
                            .spawn();

                    Vector direction = target.getLocation()
                            .add(0, target.getHeight() / 2, 0)
                            .toVector()
                            .subtract(projectile.getLocation().toVector())
                            .normalize();

                    projectile.setVelocity(direction);
                }
            }
        }.runTaskTimer(champions, 0, 2);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        LivingEntity damagee = event.getDamagee();
        UUID damagerUUID = damager.getUniqueId();

        if (bullsEyeData.get(damagerUUID) == null) return;

        LivingEntity target = bullsEyeData.get(damagerUUID).getTarget();
        if (!damagee.equals(target)) return;

        int playerLevel = getLevel(damager);
        bullsEyeData.keySet().removeIf(playerUUID -> damager == Bukkit.getPlayer(playerUUID));
        double extraDamage = getBonusDamage(playerLevel);
        double damage =  extraDamage + (event.getDamage());
        event.setDamage(damage);

        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_VILLAGER_WORK_FLETCHER, 2f, 1.2f);
        UtilMessage.simpleMessage(damagee, getName(), "<alt2>" + damager.getName() + "</alt2> hit you with <alt>" + getName());
        UtilMessage.simpleMessage(damager, getName(), "You hit <alt2>" + damagee.getName() + "</alt2> with <alt>" + getName() + "</alt> for <alt>" + String.format("%.1f", extraDamage) + "</alt> extra damage");

        //remove arrow so that it cannot hit multiple times
        arrow.remove();

        //apply cooldown
        championsManager.getCooldowns().removeCooldown(damager, getName(), true);
        championsManager.getCooldowns().use(damager,
                getName(),
                getCooldown(playerLevel),
                true,
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamageEvent(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!(cde.getProjectile() instanceof Arrow arrow)) return;
        if (!(cde.getDamager() instanceof Player damager)) return;

        // Only want to cancel this event for friendlies
        if (!UtilEntity.isEntityFriendly(damager, cde.getDamagee())) return;

        UUID playerUUID = damager.getUniqueId();
        BullseyeData data = bullsEyeData.get(playerUUID);

        if (data == null) return;
        if (!data.getArrow().equals(arrow)) return;


        arrow.remove();
        bullsEyeData.remove(playerUUID);
        event.setCancelled(true);
    }

    @UpdateEvent
    public void removeExpiredData() {
        for (UUID playerUUID : bullsEyeData.keySet()) {
            BullseyeData data = bullsEyeData.get(playerUUID);

            if (System.currentTimeMillis() >= data.getExpirationDate()) {
                bullsEyeData.remove(playerUUID);
                Player player = Bukkit.getPlayer(playerUUID);
                if (player == null) continue;
                UtilMessage.simpleMessage(player, "Bullseye", "Your target has expired.");
            }
        }
    }

    /**
     * Uses a ray trace to determine if the player is looking at the entity
     * @param player the user of this skill
     * @return null if no entity is found; otherwise the target is returned
     */
    private @Nullable LivingEntity getPotentialTarget(Player player) {
        // Perform a ray trace with a hitbox size
        int level = getLevel(player);
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                getEffectiveDistance(level),
                hitboxSize,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        // Check if the ray trace hit an entity
        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }

        return null;
    }


    public void spawnFocusingParticles(Player caster, LivingEntity target) {
        float charge = getLevel(caster) * 0.5F;

        Location casterLocation = caster.getLocation().add(0, caster.getHeight() / 3, 0);
        Location targetLocation = target.getLocation().add(0, target.getHeight() / 3, 0);

        Vector direction = targetLocation.toVector().subtract(casterLocation.toVector()).normalize();
        Vector rotatedDirection = new Vector(-direction.getZ(), direction.getY(), direction.getX()).normalize();


        double circleRadius = 0.5 - (charge/ 5);
        double offsetX = circleRadius * rotatedDirection.getX();
        double offsetY = circleRadius * rotatedDirection.getY();
        double offsetZ = circleRadius * rotatedDirection.getZ();

        Location particleLocation = targetLocation.clone().subtract(offsetX, offsetY, offsetZ).subtract(direction.multiply(2));

        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
            Vector offset = rotatedDirection.clone().multiply(circleRadius * Math.cos(angle));
            offset.setY(Math.sin(angle) * circleRadius);
            particleLocation.add(offset);
            caster.spawnParticle(Particle.DUST, particleLocation, 1, new Particle.DustOptions(Color.GREEN, 2));
        }
    }

    @Override
    public void loadSkillConfig() {
        baseCurveDistance = getConfig("baseCurveDistance", 1.0, Double.class);
        enemyCurveDistanceIncreasePerLevel = getConfig("enemyCurveDistanceIncreasePerLevel", 0.5, Double.class);
        friendlyCurveDistanceIncreasePerLevel = getConfig("friendlyCurveDistanceIncreasePerLevel", 1.5, Double.class);

        baseBonusDamage = getConfig("baseBonusDamage", 2.0, Double.class);
        baseBonusDamageIncreasePerLevel = getConfig("baseBonusDamageIncreasePerLevel", 1.0, Double.class);

        hitboxSize = getConfig("hitboxSize", 2.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
        expireTime = getConfig("expireTime", 6.0, Double.class);

        effectiveDistance = getConfig("effectiveDistance", 20.0, Double.class);
        effectiveDistanceIncreasePerLevel = getConfig("effectiveDistanceIncreasePerLevel", 0.0, Double.class);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class BullseyeData {
        final long expirationDate;
        final LivingEntity target;
        Arrow arrow;
        boolean isFriendly;
    }
}
