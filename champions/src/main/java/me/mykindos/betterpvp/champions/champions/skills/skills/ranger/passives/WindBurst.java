package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WindBurst extends Skill implements PassiveSkill, MovementSkill, DamageSkill {

    private double cooldownDecreasePerLevel;
    private double damageIncreasePerLevel;
    private double damage;
    private double radius;
    private double velocity;
    private double radiusIncreasePerLevel;
    private WeakHashMap<Player, Boolean> doubleJumpStatus;
    private WeakHashMap<Player, Long> lastJumpTime;

    @Inject
    private CooldownManager cooldownManager;

    @Inject
    public WindBurst(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        this.doubleJumpStatus = new WeakHashMap<>();
        this.lastJumpTime = new WeakHashMap<>();
    }

    @Override
    public String getName() {
        return "Wind Burst";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Crouch while Jumping to leap in the direction you",  //q skill
                "are looking and create a wind explosion below",
                "you that deals " + getValueString(this::getDamage, level) + " damage to enemies",
                "within " + getValueString(this::getRadius, level) + " blocks",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getCooldown(int level){
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public double getDamage(int level){
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getRadius(int level){
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOnGround()) {
            doubleJumpStatus.put(player, true);
            lastJumpTime.put(player, System.currentTimeMillis());
            return;
        }

        if (!doubleJumpStatus.getOrDefault(player, false)) {
            return;
        }

        if (player.getVelocity().getY() > 0 && (System.currentTimeMillis() - lastJumpTime.getOrDefault(player, 0L)) > 200) {
            doubleJumpStatus.put(player, false);
            performDoubleJumpAction(player);
        }
    }

    private void performDoubleJumpAction(Player player) {
        if(getLevel(player) <= 0) return;
        if (cooldownManager.hasCooldown(player, getName())) {
            cooldownManager.informCooldown(player, getName());
            return;
        }
        Vector direction = player.getLocation().getDirection();
        direction.setY(0).normalize();

        player.setVelocity(direction.multiply(velocity));
        VelocityData velocityData = new VelocityData(direction, velocity, false, 0.0D, 0.4D, 0.6D, false);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

        int level = getLevel(player);

        Location location = player.getLocation();
        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, location, getRadius(level));
        for (LivingEntity enemy : enemies) {
            UtilDamage.doCustomDamage(new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Wind Burst"));
        }

        player.setFallDistance(0);
        cooldownManager.use(player, getName(), getCooldown(level), true, true, true, true);

        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            double xOffset = (random.nextDouble() * 2 - 1) * getRadius(level);
            double yOffset = (random.nextDouble() * 2 - 1) * getRadius(level);
            double zOffset = (random.nextDouble() * 2 - 1) * getRadius(level);
            Location particleLocation = location.clone().add(xOffset, yOffset, zOffset);
            player.getWorld().spawnParticle(Particle.GUST, particleLocation, 1);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public boolean canUse(Player player) {
        if (cooldownManager.hasCooldown(player, getName())) {
            cooldownManager.informCooldown(player, getName());
            return false;
        }
        return true;
    }

    @Override
    public void loadSkillConfig() {
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 2.0, Double.class);
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        radius = getConfig("radius", 3.0, Double.class);
        velocity = getConfig("velocity", 1.2, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
        cooldown = getConfig("cooldown", 12.0, Double.class);
    }
}
