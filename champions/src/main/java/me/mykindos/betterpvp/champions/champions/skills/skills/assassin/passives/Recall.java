package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.RecallData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Recall extends Skill implements CooldownToggleSkill, Listener {

    public static final long MARKER_MILLIS = 150;
    private final Map<Player, RecallData> data = new WeakHashMap<>();
    private double duration;
    private double health;
    private double healthIncreasePerLevel;
    private int regenerationLevel;
    private int regenerationDuration;
    private int regenerationDurationIncreasePerLevel;

    @Inject
    public Recall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Recall";
    }

    public double getHealth(int level){
        return health + ((level - 1) * healthIncreasePerLevel);
    }

    public int getRegenerationDuration(int level){
        return regenerationDuration + ((level - 1) * regenerationDurationIncreasePerLevel);
    }

    public double getDuration(){
        return duration;
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Drop your Sword / Axe to activate",
                "",
                "Rewind back in time <stat>" + duration + "</stat> seconds, cleansing",
                "yourself of all negative effects, and gaining <val>" + getHealth(level) + "</val>",
                "health and <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationLevel + 1) + "</effect> for <val>" + getRegenerationDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        data.put(player, new RecallData(this));
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        data.remove(player);
    }

    @UpdateEvent(delay = MARKER_MILLIS)
    public void updateRecallData() {
        final Iterator<Map.Entry<Player, RecallData>> iterator = data.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, RecallData> entry = iterator.next();
            final Player player = entry.getKey();
            final RecallData recallData = entry.getValue();
            final int level = getLevel(player);
            if (!player.isOnline() || level <= 0) {
                iterator.remove();
                continue;
            }

            recallData.push(player.getLocation());
        }
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1d) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean canUse(Player player) {
        return data.get(player) != null;
    }

    @Override
    public void toggle(Player player, int level) {
        RecallData recallData = data.get(player);
        Preconditions.checkNotNull(recallData, "Recall data is null for player " + player.getName());
        final LinkedList<Location> markers = new LinkedList<>(recallData.getMarkers());
        markers.removeIf(location -> !location.getWorld().equals(player.getWorld()));

        double healAmount = getHealth(level);
        double newHealth = Math.min(player.getHealth() + healAmount, UtilPlayer.getMaxHealth(player));
        player.setHealth(newHealth);

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, getRegenerationDuration(level) * 20, regenerationLevel));
        championsManager.getEffects().addEffect(player, EffectType.RECALLING, (long) recallData.getMarkers().size() * 50L);
        championsManager.getEffects().addEffect(player, EffectType.INVISIBILITY, (long) recallData.getMarkers().size() * 50L);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);

        if (markers.isEmpty()) {
            return;
        }

        Iterator<Location> iterator = markers.iterator();
        new BukkitRunnable() {
            Location significantLocation = player.getLocation(); // Start with the players current location

            @Override
            public void run() {
                boolean foundDistantLocation = false;
                while (iterator.hasNext()) {
                    Location currentLocation = iterator.next();
                    if (significantLocation.distance(currentLocation) > 2) {
                        // Draw particle trail from significantLocation to currentLocation
                        drawParticleTrail(significantLocation, currentLocation);
                        // Teleport the player to the current location
                        teleportPlayer(player, currentLocation);
                        significantLocation = currentLocation; // Update the last significant location
                        foundDistantLocation = true;
                        break; // Exit the loop as we found a distant location
                    }
                }

                if (!foundDistantLocation) {
                    // If no further significant location found, draw particle trail to final location and teleport
                    if (!significantLocation.equals(markers.getLast())) { // Ensure it's not the same as the last significant location
                        drawParticleTrail(significantLocation, markers.getLast());
                        teleportPlayer(player, markers.getLast());
                    }
                    this.cancel(); // Cancel the task as we're done processing
                    clearEffects(player, recallData); // Clear effects and recall data
                }
            }

            private void teleportPlayer(Player player, Location location) {
                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();
                location.setYaw(yaw);
                location.setPitch(pitch);
                player.teleportAsync(location);
            }

            private void clearEffects(Player player, RecallData recallData) {
                championsManager.getEffects().removeEffect(player, EffectType.RECALLING);
                championsManager.getEffects().removeEffect(player, EffectType.INVISIBILITY);
                UtilServer.callEvent(new EffectClearEvent(player));
                recallData.getMarkers().clear();
            }

            private void drawParticleTrail(Location from, Location to) {
                final VectorLine line = VectorLine.withStepSize(from, to, 0.2);
                for (Location location : line.toLocations()) {
                    from.getWorld().spawnParticle(Particle.SPELL_WITCH, location, 3, 0, 0.3, 0, 0);
                }
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    @Override
    public void loadSkillConfig(){
        duration = getConfig("duration", 2.5, Double.class);
        health = getConfig("health", 5.0, Double.class);
        healthIncreasePerLevel = getConfig("healthIncreasePerLevel", 1.0, Double.class);
        regenerationLevel = getConfig("regenerationLevel", 1, Integer.class);
        regenerationDuration = getConfig("regenerationDuration", 3, Integer.class);
        regenerationDurationIncreasePerLevel = getConfig("regenerationDurationIncreasePerLevel", 1, Integer.class);

    }
}