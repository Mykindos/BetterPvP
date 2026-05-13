package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.ResonanceRelayProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ResonanceRelay extends Skill implements PassiveSkill, DamageSkill, BuffSkill, TeamSkill {

    private final Map<Player, ResonanceRelayProjectile> playersUsingSkill = new WeakHashMap<>();

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
                "Your bow shoots a beam that",
                "deals " + getValueString(this::getDamage, level) + " damage to enemies.",
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

        if (playersUsingSkill.containsKey(player)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot shoot your bow yet!");
            event.setCancelled(true);
            return;
        }

        // TODO: consume energy per shot

        final double energyCost = getEnergyPerShot(level);
        if (!championsManager.getEnergy().use(player, getName(), energyCost, true)) {
            event.setCancelled(true);
            return;
        }

        hitBoxSize = 0.5;
        final @NotNull ResonanceRelayProjectile projectile = shootProjectile(player, level);
        playersUsingSkill.put(player, projectile);

        arrow.remove();
    }

    private @NotNull ResonanceRelayProjectile shootProjectile(Player player, int level) {
        final @NotNull Location eyeLocation = player.getEyeLocation();
        final ResonanceRelayProjectile projectile = new ResonanceRelayProjectile(
                player,
                hitBoxSize,
                eyeLocation,
                aliveTime,
                championsManager.getEffects(),
                getAllyBuffDuration(level),
                allyBuffAmplifier,
                getDamage(level),
                maxDistance,
                this
        );

        projectile.redirect(eyeLocation.getDirection().multiply(100.0));
        return projectile;
    }

    @UpdateEvent
    public void doUpdate() {
        final Iterator<Player> iterator = playersUsingSkill.keySet().iterator();
        while (iterator.hasNext()) {

            final @NotNull Player player = iterator.next();
            if (!player.isOnline() || !hasSkill(player) || player.isDead() || !player.isValid()) {
                iterator.remove();
                continue;
            }

            final @NotNull ResonanceRelayProjectile projectile = playersUsingSkill.get(player);
            if (projectile.isMarkForRemoval() || projectile.isExpired()) {
                iterator.remove();
                continue;
            }

            projectile.tick();
        }
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
        hitBoxSize = getConfig("hitBoxSize", 1.5, Double.class);
        aliveTime = getConfig("aliveTime", 2000L, Long.class);
        speed = getConfig("speed", 100.0, Double.class);
        damage = getConfig("damage", 3.0, Double.class);
        allyBuffDuration = getConfig("allyBuffDuration", 3.0, Double.class);
        allyBuffAmplifier = getConfig("allyBuffAmplifier", 1, Integer.class);
        allyEnergyRestored = getConfig("allyEnergyRestored", 10.0, Double.class);
        maxDistance = getConfig("maxDistance", 32.0, Double.class);
        energyPerShot = getConfig("energyPerShot", 40.0, Double.class);
        energyPerShotIncreasePerLevel = getConfig("energyPerShotIncreasePerLevel", 5.0, Double.class);
    }
}
