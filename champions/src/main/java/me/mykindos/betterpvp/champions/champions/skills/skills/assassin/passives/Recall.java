package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.RecallData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Recall extends Skill implements ToggleSkill, CooldownSkill, Listener {

    public static final long MARKER_MILLIS = 200;

    private final Map<Player, RecallData> data = new WeakHashMap<>();
    private int regenerationLevel;
    private int regenerationDuration;
    private double duration;
    private double durationIncreasePerLevel;
    private int regenerationDurationIncreasePerLevel;

    @Inject
    public Recall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Recall";
    }

    public double getDuration(int level) {
        return duration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Drop your Sword / Axe to activate",
                "",
                "Teleports you back in time <val>" + getDuration(level) + "</val> seconds, giving",
                "you <effect> " + UtilFormat.getRomanNumeral(regenerationLevel + 1)  +"</effect> for <stat>" + getRegenerationTime(level) + "%</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public int getRegenerationTime(int level){
        return regenerationDuration + level * regenerationDurationIncreasePerLevel;
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        data.put(player, new RecallData(this, getLevel(player)));
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
        return SkillType.PASSIVE_B;
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
        final LinkedList<Location> markers = recallData.getMarkers();
        markers.removeIf(location -> !location.getWorld().equals(player.getWorld()));

        if (markers.isEmpty()) {
            markers.add(player.getLocation()); // Teleport them to self if they have no markers
        }

        // Teleport Logic
        Location teleportLocation = markers.getLast();
        player.teleportAsync(teleportLocation);

        // Heal Logic
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration * 20, regenerationLevel));

        // Cues
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);
        teleportLocation.getWorld().playSound(teleportLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);

        final ListIterator<Location> iterator = markers.listIterator();
        Location particleLocation = iterator.next(); // Start location - we know it exists
        while (iterator.hasNext()) {
            final Location next = iterator.next();

            final VectorLine line = VectorLine.withStepSize(particleLocation, next, 0.2);
            for (Location location : line.toLocations()) {
                Particle.SPELL_WITCH.builder()
                        .offset(0, 0.3, 0)
                        .location(location)
                        .receivers(60, true)
                        .extra(0)
                        .count(3)
                        .spawn();
            }

            particleLocation = next;
        }

        recallData.getMarkers().clear();
    }

    @Override
    public void loadSkillConfig(){
        regenerationDuration = getConfig("percentHealthRecovered", 5, Integer.class);
        regenerationLevel = getConfig("regenerationLevel", 1, Integer.class);
        duration = getConfig("duration", 2.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        regenerationDurationIncreasePerLevel = getConfig("regenerationDurationIncreasePerLevel", 0, Integer.class);
    }
}
