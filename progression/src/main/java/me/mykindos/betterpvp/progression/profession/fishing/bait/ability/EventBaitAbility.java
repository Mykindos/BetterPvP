package me.mykindos.betterpvp.progression.profession.fishing.bait.ability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.FishHook;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * Ability for the Event Bait item.
 * Similar to speedy bait but with special particle effects.
 */
@Singleton
@EqualsAndHashCode(callSuper = true)
public class EventBaitAbility extends BaitAbility {
    
    private final Progression plugin;

    /**
     * Creates a new event bait ability
     *
     * @param cooldownManager The cooldown manager
     * @param plugin The progression plugin
     */
    @Inject
    public EventBaitAbility(CooldownManager cooldownManager, Progression plugin) {
        super("Event Bait", "Reduces the waiting time for fish to bite. This bait has a bigger radius than usual.", cooldownManager);
        this.plugin = plugin;
    }

    @Override
    protected Bait createBait() {
        return new Bait(getDuration()) {
            @Override
            public String getType() {
                return "Event";
            }

            @Override
            public Material getMaterial() {
                return Material.LIGHT_BLUE_GLAZED_TERRACOTTA;
            }

            @Override
            public double getRadius() {
                return radius;
            }

            @Override
            protected void onTrack(FishHook hook) {
                hook.setWaitTime((int) (hook.getWaitTime() / multiplier));
            }

            @Override
            public int getParticleInterval() {
                return 40;
            }

            @Override
            public void doParticles() {
                new BukkitRunnable() {
                    final double rIncrement = 1.0;
                    double r = rIncrement;
                    int count = 0;
                    final Location center = floatingEntity.getLocation();
                    final int colorIncrement = (int)((double) 255 / 15 * rIncrement);

                    final Collection<org.bukkit.entity.Player> receivers = center.getWorld().getNearbyPlayers(center, 48);

                    @Override
                    public void run() {
                        for (int degree = 0; degree < 360; degree += 10) {
                            double addX = r * Math.sin(Math.toRadians(degree));
                            double addY = 0.2;
                            double addZ = r * Math.cos(Math.toRadians(degree));
                            Location newLocation = new Location(center.getWorld(), center.getX() + addX, center.getY() + addY, center.getZ() + addZ);
                            if (r < 15) {
                                Particle.DUST.builder()
                                        .data(new Particle.DustOptions(org.bukkit.Color.fromRGB(0, Math.max(125, 0), Math.max(255 - colorIncrement * count, 0)), 1.5f))
                                        .location(newLocation)
                                        .receivers(receivers)
                                        .spawn();
                            } else {
                                this.cancel();
                            }
                        }
                        r += rIncrement;
                        count++;
                    }
                }.runTaskTimer(plugin, 0, 1);
            }
        };
    }
} 