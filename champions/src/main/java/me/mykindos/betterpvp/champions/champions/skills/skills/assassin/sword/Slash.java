package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
@BPvPListener
public class Slash extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double distance;
    private double cooldownReductionPerHit;
    private double perHitReductionPerLevelIncrease;
    public double damage;
    public double damageIncreasePerLevel;
    @Inject
    private CooldownManager cooldownManager;

    @Inject
    public Slash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Slash";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Dash forwards <stat>" + distance + "</stat> blocks, dealing <val>" + getDamage(level) + "</val>",
                "damage to anything you pass through",
                "",
                "Every hit will reduce the cooldown by <stat>" + cooldownReductionPerHit + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)};
    }

    public double getDamage(int level){
        return damage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        Location originalLocation = player.getLocation();
        final Vector direction = player.getEyeLocation().getDirection();
        Location teleportLocation = originalLocation.clone();

        final int iterations = (int) Math.ceil(distance / 0.2);
        for (int i = 1; i <= iterations; i++) {
            final Vector increment = direction.clone().multiply(0.2 * i);
            final Location checkLocation = originalLocation.clone().add(increment);

            for (Entity entity : player.getWorld().getNearbyEntities(checkLocation, 0.5, 0.5, 0.5)) {
                if (entity != player) {
                    LivingEntity target = (LivingEntity) entity;

                    CustomDamageEvent cde = new CustomDamageEvent(target, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Slash");
                    UtilDamage.doCustomDamage(cde);
                    target.getWorld().playSound(target.getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT, 0.2f, 2f);
                    target.getWorld().playSound(target.getLocation().add(0, 1, 0), Sound.ITEM_TRIDENT_HIT, 0.2f, 1.5f);

                    break;
                }
            }

            BoundingBox relativeBoundingBox = UtilLocation.copyAABBToLocation(player.getBoundingBox(), checkLocation);

            final Location blockOnTop = checkLocation.clone().add(0, 1.0, 0);
            if (wouldCollide(blockOnTop.getBlock(), relativeBoundingBox)) {
                break;
            }

            Location newTeleportLocation = checkLocation;
            if (wouldCollide(checkLocation.getBlock(), relativeBoundingBox)) {
                if (!blockOnTop.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()) {
                    break;
                }

                final Vector horizontalIncrement = increment.clone().setY(0);
                final Location frontLocation = originalLocation.clone().add(horizontalIncrement);
                relativeBoundingBox = UtilLocation.copyAABBToLocation(player.getBoundingBox(), frontLocation);
                if (wouldCollide(frontLocation.getBlock(), relativeBoundingBox)) {
                    continue;
                }

                newTeleportLocation = frontLocation;
            }

            final Location headBlock = checkLocation.clone().add(0.0, relativeBoundingBox.getHeight(), 0.0);
            if (wouldCollide(headBlock.getBlock(), relativeBoundingBox)) {
                break;
            }

            if (!player.hasLineOfSight(checkLocation) && !player.hasLineOfSight(headBlock)) {
                break;
            }

            teleportLocation = newTeleportLocation;
        }

        player.leaveVehicle();
        teleportLocation = UtilLocation.shiftOutOfBlocks(teleportLocation, player.getBoundingBox());

        Particle.SWEEP_ATTACK.builder().location(teleportLocation).count(1).receivers(30).extra(0).spawn();

        final Location lineStart = player.getLocation().add(0.0, player.getHeight() / 2, 0.0);
        final Location lineEnd = teleportLocation.clone().add(0.0, player.getHeight() / 2, 0.0);
        final VectorLine line = VectorLine.withStepSize(lineStart, lineEnd, 0.25f);
        for (Location point : line.toLocations()) {
            Particle.CRIT.builder().location(point).count(2).receivers(30).extra(0).spawn();
        }

        player.teleportAsync(teleportLocation);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getDamagee() instanceof Player)) return;
        if (event.isCancelled()) return;

        boolean isEntityAttack = event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
        boolean isDashReason = Arrays.stream(event.getReason()).anyMatch(reason -> reason.equals("Slash"));

        if (!isEntityAttack && !isDashReason) return;

        cooldownManager.reduceCooldown(player, getName(), cooldownReductionPerHit);
    }


    private boolean wouldCollide(Block block, BoundingBox boundingBox) {
        return !block.isPassable() && UtilBlock.doesBoundingBoxCollide(boundingBox, block);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 2.0, Double.class);
        distance = getConfig("distance", 5.0, Double.class);
        cooldownReductionPerHit = getConfig("cooldownReductionPerHit", 4.0, Double.class);
    }
}
