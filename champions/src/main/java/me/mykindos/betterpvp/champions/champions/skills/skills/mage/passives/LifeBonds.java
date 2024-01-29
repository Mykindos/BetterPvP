package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


@Singleton
@BPvPListener
public class LifeBonds extends ActiveToggleSkill implements EnergySkill {

    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double healCooldown;
    private double healSpeed;
    private HashMap<UUID, Double> healthStored = new HashMap<>();
    private HashMap<UUID, Long> lastHealTime = new HashMap<>();
    private HashMap<UUID, BukkitRunnable> trackingTrails = new HashMap<>();
    private HashMap<UUID, Double> currentRotationAngle = new HashMap<>();

    @Inject
    public LifeBonds(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Life Bonds";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Connect you and all your allies through",
                "the power of nature, spreading your health",
                "between all allies within <val>" + getRadius(level) + "</val> blocks",
                "",
                "Energy / Second: <val>" + getEnergy(level)

        };
    }

    public double getRadius(int level) {
        return baseRadius + level * radiusIncreasePerLevel;
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            sendState(player, false);
        } else {
            if (championsManager.getEnergy().use(player, getName(), 10, false)) {
                active.add(player.getUniqueId());
                sendState(player, true);
            }

        }
    }

    @UpdateEvent(delay = 1000)
    public void audio() {
        for (UUID uuid : active) {
            Player cur = Bukkit.getPlayer(uuid);
            if (cur != null) {
                cur.getWorld().playSound(cur.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.0F);
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                int level = getLevel(player);
                if (level <= 0 || !championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true) || championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                    iterator.remove();
                } else {
                    double distance = getRadius(level);
                    findAndHealLowestHealthPlayer(player, distance);
                }
            } else {
                iterator.remove();
            }
        }
    }

    private void findAndHealLowestHealthPlayer(Player caster, double distance) {
        List<KeyValue<Player, EntityProperty>> nearbyPlayerKeyValues = UtilPlayer.getNearbyPlayers(caster, caster.getLocation(), distance, EntityProperty.FRIENDLY);

        Player highestHealthPlayer = caster;
        Player lowestHealthPlayer = caster;
        double highestHealth = caster.getHealth();
        double lowestHealth = caster.getHealth();

        for (KeyValue<Player, EntityProperty> keyValue : nearbyPlayerKeyValues) {
            Player p = keyValue.getKey();
            double health = p.getHealth();

            if (health > highestHealth) {
                highestHealth = health;
                highestHealthPlayer = p;
            }
            if (health < lowestHealth) {
                lowestHealth = health;
                lowestHealthPlayer = p;
            }
        }

        double healthDifference = highestHealth - lowestHealth;

        if (healthDifference >= 2 && (highestHealthPlayer.getHealth() - healthDifference/2) > 0) {
            long currentTime = System.currentTimeMillis();
            long lastHeal = lastHealTime.getOrDefault(lowestHealthPlayer.getUniqueId(), 0L);
            if (currentTime - lastHeal > (healCooldown * 1000L)) {
                double healthToTransfer = healthDifference / 2;
                highestHealthPlayer.setHealth(highestHealthPlayer.getHealth() - healthToTransfer);
                healthStored.put(lowestHealthPlayer.getUniqueId(), healthToTransfer);
                lastHealTime.put(lowestHealthPlayer.getUniqueId(), currentTime);
                createTrackingTrail(highestHealthPlayer, lowestHealthPlayer);
            }
        }
    }

    @UpdateEvent
    public void createRings() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Location center = player.getLocation().add(0, 1.0, 0);
                double radius = getRadius(getLevel(player));
                int numberOfPoints = 2;
                double angleIncrement = 360.0 / numberOfPoints;

                double currentAngle = currentRotationAngle.getOrDefault(uuid, 0.0);

                for (int i = 0; i < numberOfPoints; i++) {
                    double ringAngle = currentAngle + angleIncrement * i;
                    double bottomX = center.getX() + radius * Math.cos(Math.toRadians(ringAngle));
                    double bottomZ = center.getZ() + radius * Math.sin(Math.toRadians(ringAngle));
                    Location bottomRingLocation = new Location(center.getWorld(), bottomX, center.getY(), bottomZ);
                    center.getWorld().spawnParticle(Particle.CHERRY_LEAVES, bottomRingLocation, 1, 0, 0, 0, 0);

                }
                currentRotationAngle.put(uuid, (currentAngle + 6) % 360);
            }
        }
    }



    private void createTrackingTrail(Player source, Player target) {
        BukkitRunnable trailTask = new BukkitRunnable() {
            Location currentLocation = source.getLocation().add(0, 1.5, 0);

            @Override
            public void run() {
                if (!target.isOnline() || !healthStored.containsKey(target.getUniqueId())) {
                    trackingTrails.remove(source.getUniqueId());
                    this.cancel();
                    return;
                }

                if (source.getLocation().distance(target.getLocation()) > getRadius(getLevel(source))) {
                    trackingTrails.remove(source.getUniqueId());
                    this.cancel();
                    return;
                }

                Vector direction = target.getLocation().add(0, 1.5, 0).subtract(currentLocation).toVector().normalize().multiply(healSpeed);
                currentLocation.add(direction);

                source.getWorld().spawnParticle(Particle.CHERRY_LEAVES, currentLocation, 1, 0.1, 0.1, 0.1, 0);

                if (currentLocation.distance(target.getLocation().add(0, 1.5, 0)) <= healSpeed) {
                    double healthToAdd = healthStored.remove(target.getUniqueId());
                    target.setHealth(Math.min(target.getHealth() + healthToAdd, target.getMaxHealth()));
                    target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                    trackingTrails.remove(source.getUniqueId());
                    this.cancel();
                }
            }
        };

        trailTask.runTaskTimer(champions, 1L, 1L);
        trackingTrails.put(source.getUniqueId(), trailTask);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    private void sendState(Player player, boolean state) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "Life Bonds: %s", state ? "<green>On" : "<red>Off");
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 2.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);
        healCooldown = getConfig("healCooldown", 3.0, Double.class);
        healSpeed = getConfig("healSpeed", 0.4, Double.class);
    }
}