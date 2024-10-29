package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class BarbedArrows extends Skill implements PassiveSkill, DamageSkill {

    private final Map<UUID, WeakHashMap<LivingEntity, BarbedTargetData>> playerToTargetsMap = new HashMap<>();
    private final WeakHashMap<Projectile, Location> barbedProjectiles = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double damageResetTime;
    private int slownessStrength;
    private double slowDuration;

    @Inject
    public BarbedArrows(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Barbed Arrows";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hitting an arrow will stick a barb into the target",
                "melee hits on that target will rip the barb out,",
                "dealing " + getValueString(this::getDamage, level) + " extra damage and giving the target",
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> for " + getValueString(this::getSlowDuration, level) + " second",
                "",
                "The barb will fall out after " + getValueString(this::getDamageResetTime, level) + " seconds"
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    public double getDamageResetTime(int level) {
        return damageResetTime;
    }

    public double getSlowDuration(int level) {
        return slowDuration;
    }

    public int getSlownessStrength(int level) {
        return slownessStrength;
    }

    private boolean isValidProjectile(Projectile projectile) {
        return projectile instanceof Arrow || projectile instanceof Trident;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    private static class BarbedTargetData {
        double damage;
        long hitTime;

        public BarbedTargetData(double damage, long hitTime) {
            this.damage = damage;
            this.hitTime = hitTime;
        }
    }

    @EventHandler
    public void onProjectileHit(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        if (!isValidProjectile(projectile)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            LivingEntity damagee = event.getDamagee();
            UUID playerUuid = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            double damage = getDamage(level);

            playerToTargetsMap.computeIfAbsent(playerUuid, k -> new WeakHashMap<>())
                    .put(damagee, new BarbedTargetData(damage, currentTime));

            barbedProjectiles.remove(projectile);
            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        int level = getLevel(player);

        if (level > 0) {
            UUID playerUuid = player.getUniqueId();
            Map<LivingEntity, BarbedTargetData> targetsMap = playerToTargetsMap.get(playerUuid);
            if (targetsMap == null) {
                return;
            }

            BarbedTargetData barbedData = targetsMap.get(event.getDamagee());
            if (barbedData == null) {
                return;
            }

            double extraDamage = barbedData.damage;
            event.addReason(getName());
            event.setDamage(event.getDamage() + extraDamage);
            championsManager.getEffects().addEffect(event.getDamagee(), EffectTypes.SLOWNESS, slownessStrength, (long) slowDuration * 1000L);

            UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> dealt <alt2>%s</alt2> extra damage", getName(), extraDamage);
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);

            targetsMap.remove(event.getDamagee());
            if (targetsMap.isEmpty()) {
                playerToTargetsMap.remove(playerUuid);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Projectile projectile) {
            if (event.getHitBlock() != null || event.getHitEntity() == null) {
                barbedProjectiles.entrySet().removeIf(entry -> entry.getKey().equals(projectile));
            }

            UtilServer.runTaskLater(champions, () -> {
                barbedProjectiles.entrySet().removeIf(entry -> entry.getKey().equals(projectile));
            }, 2L);
        }
    }

    @UpdateEvent
    public void updateBarbedData() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, WeakHashMap<LivingEntity, BarbedTargetData>>> playerIterator = playerToTargetsMap.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, WeakHashMap<LivingEntity, BarbedTargetData>> playerEntry = playerIterator.next();
            UUID playerUuid = playerEntry.getKey();
            Map<LivingEntity, BarbedTargetData> targetsMap = playerEntry.getValue();

            Player player = Bukkit.getPlayer(playerUuid);
            int level = getLevel(player);

            if (player == null || level <= 0) {
                playerIterator.remove();
                continue;
            }

            double damageResetTimeMs = getDamageResetTime(level) * 1000;

            Iterator<Map.Entry<LivingEntity, BarbedTargetData>> targetIterator = targetsMap.entrySet().iterator();
            while (targetIterator.hasNext()) {
                Map.Entry<LivingEntity, BarbedTargetData> targetEntry = targetIterator.next();
                LivingEntity target = targetEntry.getKey();
                BarbedTargetData barbedData = targetEntry.getValue();

                if (currentTime - barbedData.hitTime > damageResetTimeMs) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "Your <alt>%s</alt> in %s have fallen out.", getName(), target.getName());
                    targetIterator.remove();
                }
            }

            if (targetsMap.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    @UpdateEvent
    public void updateArrowTrail() {
        Iterator<Projectile> it = barbedProjectiles.keySet().iterator();
        while (it.hasNext()) {
            Projectile next = it.next();
            if (next == null) {
                it.remove();
            } else if (next.isDead()) {
                it.remove();
            } else {
                Location location = next.getLocation();
                Particle.ENCHANTED_HIT.builder()
                        .count(1)
                        .extra(0)
                        .offset(0.0, 0.0, 0.0)
                        .location(location)
                        .receivers(30)
                        .spawn();
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            barbedProjectiles.put(arrow, arrow.getLocation());
        }
    }

    @EventHandler
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Trident trident) {
            if (trident.getShooter() instanceof Player player) {
                int level = getLevel(player);
                if (level > 0) {
                    barbedProjectiles.put(trident, trident.getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        playerToTargetsMap.remove(playerUuid);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerUuid = event.getEntity().getUniqueId();
        playerToTargetsMap.remove(playerUuid);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        for (Map<LivingEntity, BarbedTargetData> targetsMap : playerToTargetsMap.values()) {
            targetsMap.remove(entity);
        }
    }


    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        damageResetTime = getConfig("damageResetTime", 2.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
        slowDuration = getConfig("slowDuration", 1.0, Double.class);
    }
}
