package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.ResonanceRelayData;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.ResonanceRelayProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ResonanceRelay extends Skill implements PassiveSkill, DamageSkill, BuffSkill, TeamSkill, Listener {

    private final Map<Player, ResonanceRelayData> playersUsingSkill = new WeakHashMap<>();

    private double hitBoxSize;
    private long aliveTime;
    private double speed;
    private double damage;
    private double allyBuffDuration;
    private int allyBuffAmplifier;
    private double allyEnergyRestored;
    private double maxDistance;
    private double energyPerShot;
    private double energyPerShotIncreasePerLevel;

    @Inject
    public ResonanceRelay(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Every other shot, your bow shoots ",
                "a beam that deals " + getValueString(this::getDamage, level) + " damage to enemies.",
                "",
                "Allies gain <effect>Resistance</effect> for " + getValueString(this::getAllyBuffDuration, level) + " seconds,",
                "and restore " + getValueString(this::getAllyEnergyRestored, level) + " energy.",
                "",
                "Energy per shot: " + getValueString(this::getEnergyPerShot, level)
        };
    }

    public double getDamage(int level) {
        return damage;
    }

    public double getAllyBuffDuration(int level) {
        return allyBuffDuration;
    }

    public double getAllyEnergyRestored(int level) {
        return allyEnergyRestored;
    }

    public double getEnergyPerShot(int level) {
        return energyPerShot - (energyPerShotIncreasePerLevel * (level - 1));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        final int level = getLevel(player);
        if (level <= 0) return;

        final @NotNull ResonanceRelayData abilityData = playersUsingSkill.computeIfAbsent(player,
                p -> new ResonanceRelayData());

        if (!abilityData.isShootBeamNextShot()) {
            abilityData.setShootBeamNextShot(true);
            return;
        }

        final double energyCost = getEnergyPerShot(level);
        if (!championsManager.getEnergy().use(player, getName(), energyCost, true)) {
            event.setCancelled(true);
            return;
        }

        // Cleanup to ensure no memory leaks from old projectiles
        abilityData.removeProjectileAndArrow();

        final @NotNull ResonanceRelayProjectile projectile = shootProjectile(player, level, arrow);
        abilityData.setProjectile(projectile);
        abilityData.setShootBeamNextShot(false);

        // So arrow still needs to exist but we don't want it to be visible so tp it up in the sky and disable gravity
        abilityData.setShotArrow(arrow);

        arrow.setGravity(false);
        arrow.setVelocity(new Vector(0, 0, 0));
        UtilServer.runTask(champions, () -> arrow.teleport(arrow.getLocation().add(0, 1000, 0)));
    }

    private @NotNull ResonanceRelayProjectile shootProjectile(Player player, int level, @NotNull Arrow shotArrow) {
        final @NotNull Location eyeLocation = player.getEyeLocation();
        final ResonanceRelayProjectile projectile = new ResonanceRelayProjectile(
                player,
                hitBoxSize,
                eyeLocation,
                aliveTime,
                championsManager,
                getAllyBuffDuration(level),
                allyBuffAmplifier,
                getDamage(level),
                maxDistance,
                shotArrow,
                this,
                getAllyEnergyRestored(level)
        );

        projectile.redirect(eyeLocation.getDirection().multiply(speed));
        return projectile;
    }

    @UpdateEvent
    public void doUpdate() {
        final Iterator<Player> iterator = playersUsingSkill.keySet().iterator();
        while (iterator.hasNext()) {

            final @NotNull Player player = iterator.next();
            final @NotNull ResonanceRelayData abilityData = playersUsingSkill.get(player);

            if (!player.isOnline() || !hasSkill(player) || player.isDead() || !player.isValid()) {
                abilityData.removeProjectileAndArrow();
                iterator.remove();
                continue;
            }

            if (abilityData.isShootBeamNextShot()) {
                // If the player is supposed to shoot a beam on the next shot, we don't need to tick the projectile
                continue;
            }

            final @Nullable ResonanceRelayProjectile projectile = abilityData.getProjectile();
            if (projectile == null || projectile.isMarkForRemoval() || projectile.isExpired()) {
                abilityData.removeProjectileAndArrow();
                iterator.remove();
                continue;
            }

            projectile.tick();
        }
    }

    @EventHandler
    public void overrideArrowDamage(DamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow) && !(event.getProjectile() instanceof SpectralArrow)) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) return;

        final @Nullable ResonanceRelayData abilityData = playersUsingSkill.get(player);
        if (abilityData == null) return;
        if (abilityData.getProjectile() == null) return;

        event.setDamage(0);
    }

    @Override
    public String getName() {
        return "Resonance Relay";
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public void loadSkillConfig() {
        hitBoxSize = getConfig("hitBoxSize", 0.5, Double.class);
        aliveTime = getConfig("aliveTime", 2000L, Long.class);
        speed = getConfig("speed", 100.0, Double.class);
        damage = getConfig("damage", 3.0, Double.class);
        allyBuffDuration = getConfig("allyBuffDuration", 5.0, Double.class);
        allyBuffAmplifier = getConfig("allyBuffAmplifier", 1, Integer.class);
        allyEnergyRestored = getConfig("allyEnergyRestored", 10.0, Double.class);
        maxDistance = getConfig("maxDistance", 32.0, Double.class);
        energyPerShot = getConfig("energyPerShot", 50.0, Double.class);
        energyPerShotIncreasePerLevel = getConfig("energyPerShotIncreasePerLevel", 5.0, Double.class);
    }
}
