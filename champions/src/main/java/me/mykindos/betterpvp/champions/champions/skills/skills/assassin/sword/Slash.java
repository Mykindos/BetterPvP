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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@BPvPListener
public class Slash extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double distance;
    private double distanceIncreasePerLevel;
    private double cooldownReduction;
    private double cooldownReductionPerLevel;
    private double damage;
    private double damageIncreasePerLevel;

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
        return new String[] {
                "Right click with a Sword to activate",
                "",
                "Dash forwards <stat>" + UtilFormat.formatNumber(getDistance(level), 1) + "</stat> blocks, dealing <val>" + UtilFormat.formatNumber(getDamage(level), 1) + "</val>",
                "damage to anything you pass through",
                "",
                "Every hit will reduce the cooldown by <stat>" + UtilFormat.formatNumber(getCooldownDecrease(level), 1) + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getCooldownDecrease(int level) {
        return cooldownReduction + (cooldownReductionPerLevel * (level - 1));
    }

    public double getDistance(int level) {
        return distance + (distanceIncreasePerLevel * (level - 1));
    }

    public double getDamage(int level){
        return damage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        final Location originalLocation = player.getLocation();
        UtilLocation.teleportForward(player, getDistance(level), false, success -> {
            Particle.SWEEP_ATTACK.builder().location(player.getLocation()).count(1).receivers(30).extra(0).spawn();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);

            if (!success) {
                return;
            }

            final Location teleportLocation = player.getLocation();
            final Location lineStart = player.getLocation().add(0.0, player.getHeight() / 2, 0.0);
            final Location lineEnd = teleportLocation.clone().add(0.0, player.getHeight() / 2, 0.0);
            final VectorLine line = VectorLine.withStepSize(lineStart, lineEnd, 0.25f);
            for (Location point : line.toLocations()) {
                Particle.CRIT.builder().location(point).count(2).receivers(30).extra(0).spawn();
            }

            // Collision
            UtilEntity.interpolateMultiCollision(originalLocation,
                            teleportLocation,
                            0.5f,
                            ent -> UtilEntity.IS_ENEMY.test(player, ent))
                    .map(MultiRayTraceResult::stream)
                    .ifPresent(stream -> stream.map(RayTraceResult::getHitEntity)
                            .map(LivingEntity.class::cast)
                            .forEach(hit -> hit(player, level, hit)));
        });
    }

    private void hit(Player caster, int level, LivingEntity hit) {
        CustomDamageEvent cde = new CustomDamageEvent(hit, caster, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Slash");
        cde.setDamageDelay(0);
        UtilDamage.doCustomDamage(cde);

        if (!cde.isCancelled()) {
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT, 0.2f, 2f);
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ITEM_TRIDENT_HIT, 0.2f, 1.5f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && !event.hasReason("Slash")) {
            return;
        }

        int level = getLevel(player);
        this.championsManager.getCooldowns().reduceCooldown(player, getName(), getCooldownDecrease(level));
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
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.5, Double.class);
        distance = getConfig("distance", 5.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 0.0, Double.class);
        cooldownReduction = getConfig("cooldownReduction", 3.0, Double.class);
        cooldownReductionPerLevel = getConfig("cooldownReductionPerLevel", 0.0, Double.class);
    }
}
