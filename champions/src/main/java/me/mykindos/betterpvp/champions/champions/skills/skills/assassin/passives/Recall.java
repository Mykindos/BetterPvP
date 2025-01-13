package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.RecallData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Recall extends Skill implements CooldownToggleSkill, Listener, MovementSkill, HealthSkill {

    public static final long MARKER_MILLIS = 200;

    private final Map<Player, RecallData> data = new WeakHashMap<>();
    private double percentHealthRecovered;
    @Getter
    private double duration;


    @Inject
    public Recall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Recall";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Teleports you back in time <val>" + getDuration() + "</val> seconds, increasing",
                "your health by <val>" + UtilFormat.formatNumber(getPercentHealthRecovered() * 100, 0) + "</val> of your maximum health",
                "",
                "Cooldown: <val>" + getCooldown()
        };
    }

    private double getPercentHealthRecovered() {
        return percentHealthRecovered;
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
            if (!player.isOnline() || !hasSkill(player)) {
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
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public boolean canUse(Player player) {
        return data.get(player) != null;
    }

    @Override
    public void toggle(Player player) {
        RecallData recallData = data.get(player);
        Preconditions.checkNotNull(recallData, "Recall data is null for player " + player.getName());
        final LinkedList<Location> markers = recallData.getMarkers();
        markers.removeIf(location -> !location.getWorld().equals(player.getWorld()));

        if (markers.isEmpty()) {
            markers.add(player.getLocation()); // Teleport them to self if they have no markers
        }

        // Teleport Logic
        Location teleportLocation = markers.getLast();
        player.teleportAsync(teleportLocation).thenAccept(result -> player.setFallDistance(0));

        // Heal Logic
        double heal = UtilPlayer.getMaxHealth(player) * getPercentHealthRecovered();
        UtilPlayer.health(player, heal);

        // Cues
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);
        teleportLocation.getWorld().playSound(teleportLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);

        final ListIterator<Location> iterator = markers.listIterator();
        Location particleLocation = iterator.next(); // Start location - we know it exists
        while (iterator.hasNext()) {
            final Location next = iterator.next();

            final VectorLine line = VectorLine.withStepSize(particleLocation, next, 0.2);
            for (Location location : line.toLocations()) {
                Particle.WITCH.builder()
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
    public void loadSkillConfig() {
        percentHealthRecovered = getConfig("percentHealthRecovered", 0.20, Double.class);
        duration = getConfig("duration", 2.0, Double.class);
    }
}
