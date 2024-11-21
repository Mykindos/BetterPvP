package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe.bloodeffects;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

public class BloodCircleEffect {
    public static void runEffect(Location center, final double radius, Color color1, Color color2) {
        new BukkitRunnable() {
            final Collection<Player> receivers = center.getWorld().getNearbyPlayers(center, 48);
            double currentRadius = radius - 3.5;
            int i = 0;
            @Override
            public void run() {

                if (i >= 0 && i <= 1) {
                    createCircle(center, currentRadius, 360, receivers, 1, color1, color2);
                    currentRadius += 1;
                } else if (i > 2) {
                    createCircle(center, currentRadius, 11 - i, receivers, 60, color1, color2);
                }

                if (i > 10) {
                    this.cancel();
                }
                currentRadius += 0.2;
                i++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Champions.class), 0, 1);
    }

    private static void createCircle(Location center, final double radius, int modulusRange, Collection<Player> receivers, int angleFreq, Color color1, Color color2) {
        for (int degree = 0; degree <= 360; degree+=2) {
            if (!(degree % angleFreq < modulusRange || (degree % angleFreq) > angleFreq - modulusRange)) {
                continue;
            }
            double dx = radius * Math.sin(Math.toRadians(degree));
            double dz = radius * Math.cos(Math.toRadians(degree));
            Location newLoc = new Location(center.getWorld(), center.getX() + dx, center.getY(), center.getZ() + dz);
            Particle.DUST_COLOR_TRANSITION.builder()
                    .colorTransition(color1, color2)
                    .location(newLoc)
                    .receivers(receivers)
                    .spawn();
        }
    }
}


