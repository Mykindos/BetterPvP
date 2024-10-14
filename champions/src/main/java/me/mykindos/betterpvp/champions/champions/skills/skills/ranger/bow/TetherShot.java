package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
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
    private final Map<UUID, Map<LivingEntity, Bat>> tetherCenters = new HashMap<>();
    private final Map<UUID, List<LivingEntity>> tetheredEnemies = new HashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double radius;
    private double escapeDistance;
    private double damageIncreasePerLevel;
    private double damage;
    private double slowDuration;

    private double slowDurationIncreasePerLevel;

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
                "Your next arrow will ensnare enemies within " + getValueString(this::getRadius, level) + " blocks",
                "for " + getValueString(this::getDuration, level) + " seconds, hindering them from escaping",
                "",
                "If they do escape, the tether will snap, dealing",
                getValueString(this::getDamage, level) + " damage and <effect>Slowing</effect> them for " + getValueString(this::getSlowDuration, level) + " seconds",
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

    public double getSlowDuration(int level){
        return slowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    public double getEscapeDistance() {
        return escapeDistance;
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!hasSkill(player)) return;
        if (!tetherArrows.containsValue(arrow)) return;

        int level = getLevel(player);

        if (event.getHitBlock() != null) {
            Location arrowLocation = arrow.getLocation();
            player.getWorld().playSound(arrowLocation, Sound.ITEM_MACE_SMASH_AIR, 2.0F, 2.0F);
            doTether(player, arrowLocation, level);

        } else if (event.getHitEntity() != null) {
            Entity hitEntity = event.getHitEntity();
            doTether(player, hitEntity.getLocation(), level);
            player.getWorld().playSound(arrow.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 2.0F, 2.0F);
        }

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

    public void doTether(Player player, Location arrowLocation, int level) {
        cleanupTether(player.getUniqueId());

        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, arrowLocation, getRadius(level));

        Map<LivingEntity, Bat> enemyBats = new HashMap<>();

        for (LivingEntity enemy : enemies) {
            if (enemy instanceof Bat && enemy.hasMetadata("isTetherBat")) {
                continue;
            }

            Bat bat = arrowLocation.getWorld().spawn(arrowLocation, Bat.class, b -> {
                b.setInvisible(true);
                b.setInvulnerable(true);
                b.setAI(false);
                b.setSilent(true);
                b.setMetadata("isTetherBat", new FixedMetadataValue(champions, true));
            });

            UtilMessage.simpleMessage(player, getClassType().getName(), "You tethered <alt2>%s</alt2>", enemy.getName());
            UtilMessage.simpleMessage(enemy, getClassType().getName(), "<alt2>%s</alt2> tethered you", player.getName());

            bat.setLeashHolder(enemy);

            enemyBats.put(enemy, bat);
        }

        tetherCenters.put(player.getUniqueId(), enemyBats);
        tetheredEnemies.put(player.getUniqueId(), enemies);

    }

    private void cleanupTether(UUID playerId) {
        Map<LivingEntity, Bat> enemyBats = tetherCenters.remove(playerId);
        List<LivingEntity> enemies = tetheredEnemies.remove(playerId);

        if (enemyBats != null) {
            for (Bat bat : enemyBats.values()) {
                if (bat != null) {
                    bat.setLeashHolder(null);
                    bat.remove();
                }
            }
        }

        if (enemies != null) {
            enemies.clear();
        }

    }

    @UpdateEvent
    public void spawnParticles() {
        for (Map.Entry<UUID, Map<LivingEntity, Bat>> entry : tetherCenters.entrySet()) {
            Map<LivingEntity, Bat> enemyBats = entry.getValue();
            for (Map.Entry<LivingEntity, Bat> batEntry : new HashMap<>(enemyBats).entrySet()) {
                Bat bat = batEntry.getValue();
                if (bat == null) return;

                Particle.DustOptions redDust = new Particle.DustOptions(Color.fromRGB(128, 0, 0), 1.5F);
                new ParticleBuilder(Particle.DUST)
                        .location(bat.getLocation().clone().add(0, 0.4, 0))
                        .count(1)
                        .offset(0.0, 0.0, 0.0)
                        .extra(0)
                        .data(redDust)
                        .receivers(60)
                        .spawn();
            }
        }
    }



    @UpdateEvent(delay = 100)
    public void checkTether() {
        Iterator<Map.Entry<UUID, Map<LivingEntity, Bat>>> iterator = tetherCenters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Map<LivingEntity, Bat>> entry = iterator.next();
            UUID playerId = entry.getKey();
            Player player = Bukkit.getPlayer(playerId);

            if (player == null) {
                iterator.remove();
                tetheredEnemies.remove(playerId);
                continue;
            }

            Map<LivingEntity, Bat> enemyBats = entry.getValue();
            List<LivingEntity> enemies = tetheredEnemies.get(playerId);
            int level = getLevel(player);

            if (enemyBats == null) {
                iterator.remove();
                tetheredEnemies.remove(playerId);
                continue;
            }

            if (enemies == null) {
                iterator.remove();
                tetheredEnemies.remove(playerId);
                continue;
            }

            double maxTicksLived = getDuration(level) * 20;
            boolean tetherExpired = false;

            for (Map.Entry<LivingEntity, Bat> batEntry : new HashMap<>(enemyBats).entrySet()) {
                Bat bat = batEntry.getValue();
                if (bat == null) continue;
                if (bat.getTicksLived() > maxTicksLived) {
                    player.getWorld().playSound(batEntry.getKey(), Sound.ITEM_ARMOR_UNEQUIP_WOLF, 1.0F, 2.0F);
                    bat.setLeashHolder(null);
                    bat.remove();
                    tetherExpired = true;
                    enemyBats.remove(batEntry.getKey());
                }
            }

            if (tetherExpired) {
                if (enemyBats.isEmpty()) {
                    iterator.remove();
                    tetheredEnemies.remove(playerId);
                }
                continue;
            }

            Iterator<LivingEntity> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                LivingEntity enemy = enemyIterator.next();
                Bat bat = enemyBats.get(enemy);

                if (bat == null) {
                    enemyIterator.remove();
                    continue;
                }

                double distance = enemy.getLocation().distance(bat.getLocation());
                double innerRadius = getRadius(level);
                double escapeRange = getEscapeDistance();


                if (distance > innerRadius + escapeRange) {
                    bat.setLeashHolder(null);
                    bat.remove();

                    enemyIterator.remove();
                    enemyBats.remove(enemy);

                    doHitEffects(player, enemy, level);

                } else if (distance > innerRadius) {
                    Vector direction = bat.getLocation().toVector().subtract(enemy.getLocation().toVector()).normalize();
                    double magnitude = Math.min(1.0, (distance - innerRadius) / escapeRange);
                    enemy.setVelocity(direction.multiply(magnitude));
                }
            }

            if (enemyBats.isEmpty()) {
                iterator.remove();
                tetheredEnemies.remove(playerId);
            }
        }
    }

    public void doHitEffects(Player player, LivingEntity enemy, int level){
        CustomDamageEvent cde = new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Tether");
        UtilDamage.doCustomDamage(cde);
        championsManager.getEffects().addEffect(enemy, player, EffectTypes.SLOWNESS, 1, (long) (getSlowDuration(level) * 1000));
        player.getWorld().playSound(enemy.getLocation(), Sound.ITEM_ARMOR_UNEQUIP_WOLF, 1.0F, 2.0F);

        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %s</alt>.", enemy.getName(), getName(), level);
        UtilMessage.simpleMessage(enemy, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", player.getName(), getName(), level);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        cleanUp(entity);
    }

    @EventHandler
    public void onEntityTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        for (Map.Entry<UUID, List<LivingEntity>> entry : tetheredEnemies.entrySet()) {
            List<LivingEntity> enemies = entry.getValue();
            if (enemies.contains(player)) {
                Player caster = Bukkit.getPlayer(entry.getKey());
                if (caster == null) continue;

                Map<LivingEntity, Bat> enemyBats = tetherCenters.get(caster.getUniqueId());
                if (enemyBats == null || enemyBats.isEmpty()) continue;

                Bat bat = enemyBats.get(player);
                if (bat == null) continue;

                double distance = player.getLocation().distance(event.getTo());

                if (distance > getRadius(getLevel(caster)) + getEscapeDistance()) {
                    doHitEffects(caster, player, getLevel(caster));
                    cleanUp(player);
                }
                break;
            }
        }
    }


    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event){
        LivingEntity player = event.getPlayer();
        cleanUp(player);
    }

    public void cleanUp(LivingEntity entity){
        UUID playerId = null;

        for (Map.Entry<UUID, List<LivingEntity>> entry : tetheredEnemies.entrySet()) {
            if (entry.getValue().contains(entity)) {
                playerId = entry.getKey();
                break;
            }
        }

        if (playerId != null) {
            Map<LivingEntity, Bat> enemyBats = tetherCenters.get(playerId);
            if (enemyBats != null) {
                Bat bat = enemyBats.remove(entity);
                if (bat != null) {
                    bat.setLeashHolder(null);
                    bat.remove();
                }
            }
        }
    }

    @EventHandler
    public void preventBatDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Bat bat)) return;

        if (bat.hasMetadata("isTetherBat")) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent
    public void updateArrowTrail() {
        for (Arrow arrow : tetherArrows.values()) {
            displayTrail(arrow.getLocation());
        }
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.INFESTED)
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
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        radius = getConfig("radius", 5.0, Double.class);
        escapeDistance = getConfig("escapeDistance", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        damage = getConfig("damage", 6.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowDuration = getConfig("slowDuration", 4.0, Double.class);
    }
}