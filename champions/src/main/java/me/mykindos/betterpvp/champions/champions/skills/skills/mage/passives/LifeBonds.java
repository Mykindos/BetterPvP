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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;


@Singleton
@BPvPListener
public class LifeBonds extends ActiveToggleSkill implements EnergySkill {

    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double healCooldown;
    private double healSpeed;
    private double healMultiplier;

    private final HashMap<UUID, Double> healthStored = new HashMap<>();
    private final HashMap<UUID, Long> lastHealTime = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> trackingTrails = new HashMap<>();

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
                "Connect to your allies within <val>" + getRadius(level) + "</val> blocks,",
                "causing the highest health player in the",
                "radius to transfer their health to the",
                "lowest health player every <stat>" + healCooldown + "</stat> seconds",
                "",
                "Energy / Second: <val>" + getEnergy(level)

        };
    }

    public double getRadius(int level) {
        return baseRadius + level * radiusIncreasePerLevel;
    }

    @Override
    public boolean process(Player player) {
        HashMap<String, Long> updateCooldowns = updaterCooldowns.get(player.getUniqueId());

        if (updateCooldowns.getOrDefault("audio", 0L) < System.currentTimeMillis()) {
            audio(player);
            updateCooldowns.put("audio", System.currentTimeMillis() + 1000);
        }

        return onUpdate(player);
    }

    @Override
    public void toggleActive(Player player) {
        if (championsManager.getEnergy().use(player, getName(), 10, false)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "Life Bonds: <green>On");
        }
    }

    private void audio(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3F, 0.0F);
    }

    public boolean onUpdate(Player player) {

        if (player != null) {
            int level = getLevel(player);
            if (level <= 0 || !championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true) || championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                return false;
            } else {
                double distance = getRadius(level);
                findAndHealLowestHealthPlayer(player, distance);
            }
        }

        return true;
    }

    public void createParticlesForPlayers(Player caster, List<KeyValue<Player, EntityProperty>> nearbyPlayerKeyValues) {
        caster.getWorld().spawnParticle(Particle.CHERRY_LEAVES, caster.getLocation().add(0, 1.0, 0), 1, 0.1, 0.1, 0.1, 0);

        for (KeyValue<Player, EntityProperty> keyValue : nearbyPlayerKeyValues) {
            Player player = keyValue.getKey();
            player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, player.getLocation().add(0, 1.0, 0), 1, 0.1, 0.1, 0.1, 0);
        }
    }

    private void findAndHealLowestHealthPlayer(Player caster, double distance) {
        List<KeyValue<Player, EntityProperty>> nearbyPlayerKeyValues = UtilPlayer.getNearbyPlayers(caster, caster.getLocation(), distance, EntityProperty.FRIENDLY);
        createParticlesForPlayers(caster, nearbyPlayerKeyValues);

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
        double healthToTransfer = healthDifference * healMultiplier;

        if (healthToTransfer >= 2 && (highestHealthPlayer.getHealth() - healthToTransfer) > 2) {
            long currentTime = System.currentTimeMillis();
            long lastHeal = lastHealTime.getOrDefault(lowestHealthPlayer.getUniqueId(), 0L);
            if (currentTime - lastHeal > (healCooldown * 1000L)) {
                highestHealthPlayer.setHealth(highestHealthPlayer.getHealth() - healthToTransfer);
                healthStored.put(lowestHealthPlayer.getUniqueId(), healthToTransfer);
                lastHealTime.put(lowestHealthPlayer.getUniqueId(), currentTime);
                createTrackingTrail(highestHealthPlayer, lowestHealthPlayer);
            }
        }
    }

    private void createTrackingTrail(Player source, Player target) {
        BukkitRunnable trailTask = new BukkitRunnable() {
            Location currentLocation = source.getLocation().add(0, 1.5, 0);

            @Override
            public void run() {
                if (!target.isOnline() || !healthStored.containsKey(target.getUniqueId()) || target.isDead()) {
                    trackingTrails.remove(source.getUniqueId());
                    healthStored.remove(target.getUniqueId());
                    this.cancel();
                    return;
                }

                Vector direction = target.getLocation().add(0, 1.5, 0).subtract(currentLocation).toVector().normalize().multiply(healSpeed);
                currentLocation.add(direction);

                source.getWorld().spawnParticle(Particle.CHERRY_LEAVES, currentLocation, 1, 0.1, 0.1, 0.1, 0);

                if (currentLocation.distance(target.getLocation().add(0, 1.5, 0)) <= healSpeed) {
                    double healthToAdd = healthStored.remove(target.getUniqueId());
                    target.setHealth(Math.min(target.getHealth() + healthToAdd, UtilPlayer.getMaxHealth(target)));
                    target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
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
        return SkillType.PASSIVE_A;
    }


    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 2.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);
        healCooldown = getConfig("healCooldown", 2.0, Double.class);
        healSpeed = getConfig("healSpeed", 0.3, Double.class);
        healMultiplier = getConfig("healMultiplier", 0.25, Double.class);
    }
}