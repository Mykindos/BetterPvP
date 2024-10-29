package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.StackingHitData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Sharpshooter extends Skill implements PassiveSkill, DamageSkill {

    private final WeakHashMap<Player, StackingHitData> data = new WeakHashMap<>();
    private final WeakHashMap<Projectile, Location> projectiles = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private int maxConsecutiveHits;
    private int maxConsecutiveHitsIncreasePerLevel;
    private double duration;

    @Inject
    public Sharpshooter(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sharpshooter";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Each subsequent arrow hit will",
                "increase your damage by " + getValueString(this::getDamage, level),
                "",
                "Stacks up to " + getValueString(this::getMaxConsecutiveHits, level) + " times",
                "",
                "Missing a shot or taking damage will reset Sharpshooter",
        };
    }

    private boolean isValidProjectile(Projectile projectile) {
        return projectile instanceof Arrow || projectile instanceof Trident;
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    public int getMaxConsecutiveHits(int level) {
        return maxConsecutiveHits + ((level - 1) * maxConsecutiveHitsIncreasePerLevel);
    }

    public double getDuration(int level) {
        return duration;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Projectile arrow)) return;

        int level = getLevel(player);
        if (level > 0) {
            PlayerCanUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerCanUseSkillEvent(player, this));
            if (!skillEvent.isCancelled()) {
                projectiles.put(arrow, arrow.getLocation());
            }
        }
    }

    @EventHandler
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Trident trident) {
            if (trident.getShooter() instanceof Player player) {
                int level = getLevel(player);
                if (level > 0) {
                    projectiles.put(trident, trident.getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onProjectileDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!isValidProjectile(projectile)) return;

        int level = getLevel(damager);
        if (level > 0 && data.containsKey(damager)) {
            StackingHitData hitData = data.get(damager);
            double bonusDamage = Math.min(hitData.getCharge(), getMaxConsecutiveHits(level) + 1) * getDamage(level) - getDamage(level);
            event.setDamage(event.getDamage() + bonusDamage);
            event.addReason("Sharpshooter");
            if(bonusDamage > 0) {
                UtilMessage.simpleMessage(damager, getClassType().getName(), "<yellow>%d<gray> consecutive hits (<green>+%.2f damage<gray>)", hitData.getCharge(), bonusDamage);
                damager.playSound(damager.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, (0.8f + (float) (hitData.getCharge() * 0.2)));
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!isValidProjectile(projectile)) return;
        if (!(projectile.getShooter() instanceof Player shooter)) return;

        int level = getLevel(shooter);
        if (level > 0) {
            if (!data.containsKey(shooter)) {
                data.put(shooter, new StackingHitData());
            }
            projectiles.remove(projectile);

            StackingHitData hitData = data.get(shooter);

            if (event.getHitEntity() != null) {
                hitData.addCharge();
            } else {
                data.remove(shooter);
            }
        }
    }

    @UpdateEvent(delay = 100)
    public void update() {
        Iterator<Projectile> it = projectiles.keySet().iterator();
        while (it.hasNext()) {
            Projectile next = it.next();
            if (next == null) {
                it.remove();
            } else if (next.isDead()) {
                it.remove();
            } else {
                Location location = next.getLocation().add(new Vector(0, 0.25, 0));
                Particle.WAX_ON.builder().location(location).receivers(60).extra(0).spawn();
            }
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            data.remove(player);
        }
    }

    @UpdateEvent(delay = 500)
    public void updateSharpshooterData() {
        data.entrySet().removeIf(entry -> {
            if (System.currentTimeMillis() > entry.getValue().getLastHit() + (getDuration(getLevel(entry.getKey())) * 1000L)) {
                Player player = entry.getKey();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.75f);
                UtilMessage.simpleMessage(player, getClassType().getName(), "<green>%s %d<gray> has ended at <yellow>%s<gray> damage", getName(), getLevel(player), (Math.min(getMaxConsecutiveHits(getLevel(player)), data.get(player).getCharge()) * getDamage(getLevel(player))));
                return true;
            }
            return false;
        });
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 5.0, Double.class);
        maxConsecutiveHits = getConfig("maxConsecutiveHits", 5, Integer.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.25, Double.class);
        maxConsecutiveHitsIncreasePerLevel = getConfig("maxConsecutiveHitsIncreasePerLevel", 0, Integer.class);
    }
}