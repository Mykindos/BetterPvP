package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class WindDashAbility extends ItemAbility {

    private double dashVelocity;
    private double dashImpactVelocity;
    private int dashParticleTicks;
    private int dashEnergyCost;

    @EqualsAndHashCode.Exclude
    private final ChampionsManager championsManager;
    @EqualsAndHashCode.Exclude
    private final Champions champions;
    
    // Data storage for active dashes
    @EqualsAndHashCode.Exclude
    private final Map<Player, DashData> activeDashes = new HashMap<>();

    public WindDashAbility(ChampionsManager championsManager, Champions champions) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "wind_dash"),
                "Wind Dash",
                "Take a leap forward. The first enemy you hit, will be launched into the air.",
                TriggerTypes.RIGHT_CLICK);
        this.championsManager = championsManager;
        this.champions = champions;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.simpleMessage(player, getName(), "You cannot use this ability while in liquid");
            return false;
        }
        
        if (!championsManager.getEnergy().use(player, getName(), dashEnergyCost, true)) {
            return false;
        }

        // Start dash tracking
        activeDashes.put(player, new DashData());

        // Apply velocity
        Vector vec = player.getLocation().getDirection().normalize().multiply(dashVelocity);
        VelocityData velocityData = new VelocityData(vec, dashVelocity, false, 0.0D, 0.4D, 0.8D, true);
        UtilVelocity.velocity(player, player, velocityData);

        // SFX
        new SoundEffect(Sound.ITEM_TRIDENT_THROW, 0.5F, 2.0F).play(player.getLocation());

        // VFX
        UtilMessage.message(player, "Wind Blade", "You used <alt>" + getName() + "</alt>.");
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= dashParticleTicks) {
                    this.cancel();
                    return;
                }

                ticks++;
                final Player[] receivers = Bukkit.getOnlinePlayers().stream()
                        .map(p -> (Player) p)
                        .toArray(Player[]::new);
                Particle.CLOUD.builder()
                        .location(player.getLocation())
                        .count(10)
                        .receivers(receivers)
                        .offset(0.5, 0.5, 0.5)
                        .extra(0.1)
                        .spawn();
                Particle.GUST.builder()
                        .location(player.getLocation())
                        .count(1)
                        .receivers(receivers)
                        .offset(0.5, 0.5, 0.5)
                        .extra(0.1)
                        .spawn();
            }
        }.runTaskTimer(champions, 0L, 1L);
        return true;
    }

    // Call this from a scheduler in the main item class
    public void processDashes() {
        Iterator<Map.Entry<Player, DashData>> dashIterator = activeDashes.entrySet().iterator();
        while (dashIterator.hasNext()) {
            Map.Entry<Player, DashData> entry = dashIterator.next();
            DashData dash = entry.getValue();
            Player player = entry.getKey();
            if (player.isDead() || !player.isOnline()) {
                dashIterator.remove(); // Remove offline or dead players
                continue;
            }

            if (UtilBlock.isGrounded(player) && UtilTime.elapsed(dash.getStartTime(), 750L)) {
                dashIterator.remove(); // Remove grounded players after 750ms of dashing
                continue;
            }

            // Check for collisions
            final Location midpoint = UtilPlayer.getMidpoint(player).clone();
            final Optional<LivingEntity> targetOpt = UtilEntity.interpolateCollision(midpoint,
                            midpoint.clone().add(player.getVelocity().normalize().multiply(0.5)),
                            0.6f,
                            ent -> UtilEntity.IS_ENEMY.test(player, ent))
                    .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

            if (targetOpt.isEmpty()) {
                continue; // No hit
            }

            // Cancel the dash now and do the collision
            dashIterator.remove();
            final LivingEntity target = targetOpt.get();

            // Don't hit the same target twice
            if (dash.getHitTargets().contains(target)) {
                continue;
            }
            dash.getHitTargets().add(target);

            // Velocity
            Vector upwardVelocity = new Vector(0, 1, 0).multiply(dashImpactVelocity);
            UtilVelocity.velocity(target, player, new VelocityData(upwardVelocity, 1.0, 0.0, 10.0, false));

            // SFX & VFX
            new SoundEffect(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 2).play(target.getLocation());
            new SoundEffect(Sound.ENTITY_PUFFER_FISH_STING, 0.8F, 1.5F).play(target.getLocation());
            UtilMessage.message(player, "Wind Blade", "You hit <alt2>" + target.getName() + "</alt2> with <alt>" + getName() + "</alt>.");
            UtilMessage.message(target, "Wind Blade", "<alt2>" + player.getName() + "</alt2> hit you with <alt>" + getName() + "</alt>.");
        }
    }

    public static class DashData {
        private final long startTime = System.currentTimeMillis();
        private final Set<LivingEntity> hitTargets = new HashSet<>();

        public long getStartTime() {
            return startTime;
        }

        public Set<LivingEntity> getHitTargets() {
            return hitTargets;
        }
    }
} 