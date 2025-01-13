package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
@BPvPListener
public class FireBlast extends Skill implements InteractSkill, CooldownSkill, Listener, FireSkill, CrowdControlSkill, OffensiveSkill {

    private double speed;
    public final List<LargeFireball> fireballs = new ArrayList<>();
    @Getter
    private double damage;
    @Getter
    private double fireDuration;
    @Getter
    private double radius;
    private double velocityMultiplier;
    private double yAdd;
    private double yMax;
    private boolean groundBoost;
    private double fallDamageLimit;


    @Inject
    public FireBlast(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fire Blast";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Launch a fireball which explodes on impact,",
                "knocking back any players within <val>" + getRadius() + "</val> blocks",
                "dealing <val>" + getDamage() + "</val> damage, and igniting them for ",
                getFireDuration() + " seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @UpdateEvent
    public void update() {
        Iterator<LargeFireball> it = fireballs.iterator();
        while (it.hasNext()) {
            LargeFireball fireball = it.next();
            if (fireball == null || fireball.isDead()) {
                it.remove();
                continue;
            }
            if (fireball.getLocation().getY() < 255 || !fireball.isDead()) {
                Particle.LAVA.builder().location(fireball.getLocation()).receivers(30).count(1).spawn();
            } else {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof LargeFireball largeFireball) {
            fireballs.remove(largeFireball);

            if (!(largeFireball.getShooter() instanceof Player shooter)) {
                return;
            }

            if (!hasSkill(shooter)) {
                return;
            }

            UtilServer.runTaskLater(champions, () -> {
                doExplosion(shooter, largeFireball.getLocation().add(0, 1, 0));
            }, 1L);

        }
    }

    private void doExplosion(Player shooter, Location fireballLocation) {
        final List<KeyValue<LivingEntity, EntityProperty>> nearby = UtilEntity.getNearbyEntities(shooter, fireballLocation, getRadius(), EntityProperty.ALL);

        new ParticleBuilder(Particle.EXPLOSION)
                .location(fireballLocation)
                .count(1)
                .receivers(60)
                .spawn();

        double radius = getRadius();
        if (shooter.getLocation().distance(fireballLocation) <= radius && nearby.stream().noneMatch(entry -> entry.get().equals(shooter))) {
            nearby.add(new KeyValue<>(shooter, EntityProperty.FRIENDLY));
        }

        for (KeyValue<LivingEntity, EntityProperty> entry : nearby) {
            EntityProperty property = entry.getValue();
            final LivingEntity target = entry.get();
            if (target.hasLineOfSight(fireballLocation)) {
                Vector fireballVector = fireballLocation.toVector();
                Vector adjustedFireballVector = fireballVector.clone().add(new Vector(0, -2, 0));
                Vector direction = target.getLocation().toVector().subtract(adjustedFireballVector).normalize();

                VelocityData velocityData = new VelocityData(direction, velocityMultiplier, false, 0.0D, yAdd, yMax, groundBoost);
                UtilVelocity.velocity(target, shooter, velocityData, VelocityType.CUSTOM);

                double fireDuration = getFireDuration();
                if (property != EntityProperty.FRIENDLY) {

                    UtilDamage.doCustomDamage(new CustomDamageEvent(target, shooter, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(), false, "Fire Blast"));
                    UtilServer.runTaskLater(champions, () -> UtilEntity.setFire(target, shooter, (long) (1000L * fireDuration)), 2);

                }
                if (property == EntityProperty.FRIENDLY || target.equals(shooter)) {
                    UtilServer.runTaskLater(champions, () -> {
                        championsManager.getEffects().addEffect(target, shooter, EffectTypes.NO_FALL, getName(), (int) fallDamageLimit,
                                50L, true, true, UtilBlock::isGrounded);
                    }, 3L);
                }
            }
        }
    }


    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof LargeFireball largeFireball) {
            fireballs.remove(largeFireball);
            largeFireball.getWorld().playSound(largeFireball.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.0f);
        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getProjectile() != null) {
            Projectile fireball = event.getProjectile();
            if (fireball instanceof LargeFireball && fireball.getShooter() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void activate(Player player) {
        LargeFireball fireball = player.launchProjectile(LargeFireball.class, player.getLocation().getDirection().multiply(speed));
        fireball.setYield(0);
        fireball.setIsIncendiary(false);

        fireballs.add(fireball);
        fireball.getWorld().playSound(fireball.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);

    }

    @Override
    public void loadSkillConfig() {
        fallDamageLimit = getConfig("fallDamageLimit", 8.0, Double.class);
        speed = getConfig("speed", 0.2, Double.class);
        damage = getConfig("damage", 2.0, Double.class);
        fireDuration = getConfig("fireDuration", 0.8, Double.class);
        radius = getConfig("radius", 4.0, Double.class);
        velocityMultiplier = getConfig("velocityMultiplier", 2.0, Double.class);
        yAdd = getConfig("yAdd", 0.4, Double.class);
        yMax = getConfig("yMax", 0.8, Double.class);
        groundBoost = getConfig("groundBoost", true, Boolean.class);

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}