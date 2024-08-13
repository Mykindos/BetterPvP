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
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class BarbedArrows extends Skill implements PassiveSkill, DamageSkill {
    private final WeakHashMap<LivingEntity, Double> data = new WeakHashMap<>();
    private final Map<UUID, Long> arrowHitTime = new HashMap<>();
    private final WeakHashMap<Projectile, Location> barbedProjectiles = new WeakHashMap<>();
    private final Map<UUID, LivingEntity> playerToEntityMap = new HashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double damageResetTime;

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
                "dealing " + getValueString(this::getDamage, level) + " extra damage",
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

    private boolean isValidProjectile(Projectile projectile) {
        return projectile instanceof Arrow || projectile instanceof Trident;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onProjectileHit(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        if (!isValidProjectile(projectile)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            LivingEntity damagee = event.getDamagee();
            data.put(damagee, getDamage(level));
            arrowHitTime.put(player.getUniqueId(), System.currentTimeMillis());
            playerToEntityMap.put(player.getUniqueId(), damagee);
            barbedProjectiles.remove(projectile);
            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);
        }
    }



    @EventHandler
    public void onHit(CustomDamageEvent event){
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        int level = getLevel(player);

        if (level > 0) {
            if (!data.containsKey(event.getDamagee())) {
                return;
            }

            double extraDamage = data.get(event.getDamagee());
            event.addReason(getName());
            event.setDamage(event.getDamage() + extraDamage);

            UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> dealt <alt2>%s</alt2> extra damage", getName(), extraDamage);
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);
            data.remove(event.getDamagee());
            arrowHitTime.remove(player.getUniqueId());
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

        Iterator<Map.Entry<UUID, Long>> iterator = arrowHitTime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID playerUuid = entry.getKey();
            Long lastTimeHit = entry.getValue();

            Player player = Bukkit.getPlayer(playerUuid);
            int level = getLevel(player);

            if (player == null || level <= 0) {
                iterator.remove();
                playerToEntityMap.remove(playerUuid);
                continue;
            }

            if (currentTime - lastTimeHit > getDamageResetTime(level) * 1000) {
                UtilMessage.simpleMessage(player, getClassType().getName(), "Your <alt>%s</alt> have fallen out.", getName());
                iterator.remove();

                LivingEntity hitEntity = playerToEntityMap.get(playerUuid);
                if (hitEntity != null) {
                    data.remove(hitEntity);
                    playerToEntityMap.remove(playerUuid);
                }
            }
        }

        data.entrySet().removeIf(entry -> !playerToEntityMap.containsValue(entry.getKey()));
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
    public void onShoot(EntityShootBowEvent event){
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        if (!isValidProjectile(projectile)) return;
        if (!(projectile.getShooter() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            barbedProjectiles.put(projectile, projectile.getLocation());
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 1.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        damageResetTime = getConfig("damageResetTime", 2.0, Double.class);
    }
}
