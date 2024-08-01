package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class TetherShot extends PrepareArrowSkill implements InteractSkill, CooldownSkill, Listener, DebuffSkill, OffensiveSkill {

    private final Map<UUID, Arrow> tetherArrows = new HashMap<>();
    private final Map<UUID, ArmorStand> tetherCenters = new HashMap<>();
    private final Map<UUID, List<LivingEntity>> tetheredEnemies = new HashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double radius;
    private double escapeDistance;
    private double damagePerLevel;

    @Inject
    public TetherShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Tether Shot";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will ensnare enemies within x radius,",
                "preventing them from escaping. ",
                "",
                "if they use a movement ability to escape, the tether will snap",
                "dealing x damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level - 1));
    }

    public double getRadius(int level){
        return radius;
    }

    public double getEscapeDistance() {
        return escapeDistance;
    }

    public double getDamage(int level) {
        return damagePerLevel * level;
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

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!hasSkill(player)) return;
        if (!tetherArrows.containsValue(arrow)) return;

        int level = getLevel(player);
        Location arrowLocation = arrow.getLocation();

        player.getWorld().playSound(arrowLocation, Sound.ENTITY_SNIFFER_EAT, 2.0F, 2.0F);
        doTether(player, arrowLocation, level);

        tetherArrows.remove(player.getUniqueId());
        arrow.remove();
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        tetherArrows.put(player.getUniqueId(), arrow);
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        //ignore
    }

    public void doTether(Player player, Location arrowLocation, int level){
        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, arrowLocation, getRadius(level));

        // Create an invisible entity at the arrow's location
        ArmorStand center = arrowLocation.getWorld().spawn(arrowLocation, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setMarker(true);
        });

        // Attach leads from enemies to the invisible entity
        for (LivingEntity enemy : enemies) {
            enemy.setLeashHolder(center);
        }

        // Store the tether data
        tetherCenters.put(player.getUniqueId(), center);
        tetheredEnemies.put(player.getUniqueId(), enemies);
    }

    @UpdateEvent(delay = 500)
    public void checkTether() {
        for (UUID playerId : tetherCenters.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;

            ArmorStand center = tetherCenters.get(playerId);
            List<LivingEntity> enemies = tetheredEnemies.get(playerId);
            int level = getLevel(player);

            if (center == null || enemies == null) continue;

            Iterator<LivingEntity> iterator = enemies.iterator();
            while (iterator.hasNext()) {
                LivingEntity enemy = iterator.next();
                if (enemy.getWorld() != center.getWorld()) continue;

                double distance = enemy.getLocation().distance(center.getLocation());
                double radius = getRadius(level);
                double escapeDistance = getEscapeDistance();

                if (distance > radius + escapeDistance) {
                    enemy.setLeashHolder(null);
                    CustomDamageEvent cde = new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Tether");
                    UtilDamage.doCustomDamage(cde);
                    player.getWorld().playSound(enemy.getLocation(), Sound.ITEM_AXE_STRIP, 1.0F, 2.0F);
                    iterator.remove();
                } else if (distance > radius) {
                    Vector direction = center.getLocation().toVector().subtract(enemy.getLocation().toVector()).normalize();
                    double magnitude = Math.min(1.0, (distance - radius) / escapeDistance);
                    enemy.setVelocity(direction.multiply(magnitude));
                }
            }

            if (center.getTicksLived() > getDuration(level) * 20) {
                for (LivingEntity enemy : enemies) {
                    enemy.setLeashHolder(null);
                    player.getWorld().playSound(enemy.getLocation(), Sound.ITEM_AXE_STRIP, 1.0F, 2.0F);

                }
                tetherCenters.remove(playerId);
                tetheredEnemies.remove(playerId);
                center.remove();
            }
        }
    }


    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.SCULK_CHARGE_POP)
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
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        radius = getConfig("radius", 4.0, Double.class);
        escapeDistance = getConfig("escapeDistance", 2.0, Double.class);
        damagePerLevel = getConfig("damagePerLevel", 1.0, Double.class);
    }
}
