package me.mykindos.betterpvp.champions.champions.skills.skills.gladiator.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@BPvPListener
public class Intimidation extends Skill implements PassiveSkill {

    private static final int DURATION_TICKS = 10;
    private static final PotionEffect SLOWNESS = new PotionEffect(PotionEffectType.SLOW, DURATION_TICKS, 0, false, false);

    private double baseRadius;
    private final AtomicInteger soundTicks = new AtomicInteger(0);
    private final WeakHashMap<Player, Set<Player>> trackedEnemies = new WeakHashMap<>();

    @Inject
    public Intimidation(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Intimidation";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Every enemy facing away from you within <val>" + getRadius(level) + "</val>",
                "blocks will get <effect>Slowness 1",
        };
    }

    @Override
    public void trackPlayer(Player player) {
        trackedEnemies.put(player, new HashSet<>());
    }

    @Override
    public void invalidatePlayer(Player player) {
        for (Player tracked : trackedEnemies.remove(player)) {
            UtilPlayer.clearWarningEffect(tracked); // Clear them if they are no longer in front
        }
    }

    public double getRadius(int level) {
        return baseRadius + (level - 1);
    }

    @UpdateEvent
    public void onUpdate() {
        final boolean sounds = soundTicks.get() == 0;
        final Iterator<Player> iterator = trackedEnemies.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            if (player == null || !player.isOnline()) {
                iterator.remove(); // Remove if not online
                continue;
            }

            int level = getLevel(player);
            if (level < 0) {
                iterator.remove(); // Remove because unequipped
                continue;
            }

            intimidateNearby(player, level, sounds);
        }

        if (soundTicks.addAndGet(1) >= 20) {
            soundTicks.set(0); // Play sound every second
        }
    }

    public void intimidateNearby(Player player, int level, boolean sounds) {
        double radius = getRadius(level);
        List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius);
        trackedEnemies.get(player).removeIf(enemy -> {
            final boolean remove = !nearbyEnemies.contains(enemy);
            if (remove) {
                UtilPlayer.clearWarningEffect(enemy);
            }
            return remove;
        }); // Remove enemies that are no longer nearby

        for (Player enemy : nearbyEnemies) {
            if (enemy.hasLineOfSight(player) && UtilLocation.isInFront(enemy, player.getEyeLocation())) {
                trackedEnemies.get(player).add(enemy); // track
                UtilPlayer.setWarningEffect(enemy, 1);
                if (sounds) {
                    UtilSound.playSound(enemy, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f, true);
                }
                enemy.addPotionEffect(SLOWNESS);
            } else if (trackedEnemies.get(player).remove(enemy)) {
                UtilPlayer.clearWarningEffect(enemy); // Clear them if they are no longer in front
            }
        }
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig(){
        baseRadius = getConfig("baseRadius", 3.0, Double.class);
    }
}
