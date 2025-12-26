package me.mykindos.betterpvp.champions.item.thornfang;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbilityDamageCause;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class Vipersprint extends ItemAbility {

    private transient final CooldownManager cooldownManager;
    private transient final ClientManager clientManager;
    private transient final EffectManager effectManager;
    private transient final WeakHashMap<Player, Properties> active = new WeakHashMap<>();

    private double cooldown;
    private double duration;
    private double speed;
    private double damage;
    private double poisonSeconds;
    private int poisonAmplifier;

    protected Vipersprint(Champions champions, CooldownManager cooldownManager, ClientManager clientManager, EffectManager effectManager) {
        super(new NamespacedKey(champions, "vipersprint"),
                "Vipersprint",
                "Dash forward at high speed, curving your path with your aim while cutting through anything in your way. Hitting an enemy resets your cooldown.",
                TriggerTypes.HOLD_BLOCK);
        this.cooldownManager = cooldownManager;
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        UtilServer.runTaskTimer(champions, this::tick, 0, 1);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        final Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        if (!active.containsKey(player)) {
            if (this.cooldownManager.hasCooldown(player, getName())) {
                return false; // Cooldown is active
            }

            active.put(player, new Properties());
        }

        final Properties properties = active.get(player);
        if (UtilTime.elapsed(properties.getStartTime(), (long) (duration * 1000))) {
            stop(player);
            return false;
        }

        dash(player, properties);
        poisonNearby(player);
        properties.lastLocation = player.getLocation();
        return true;
    }

    private void poisonNearby(Player player) {
        final Properties properties = active.get(player);

        final Location lastLocation = properties.getLastLocation();
        final List<LivingEntity> enemies = UtilEntity.interpolateMultiCollision(lastLocation != null ? lastLocation : player.getLocation(),
                        player.getLocation(),
                        0.9f,
                        ent -> UtilEntity.IS_ENEMY.test(player, ent))
                .stream()
                .flatMap(MultiRayTraceResult::stream)
                .map(RayTraceResult::getHitEntity)
                .filter(LivingEntity.class::isInstance)
                .filter(ent -> !properties.getDamagedEntities().contains(ent.getUniqueId()))
                .map(LivingEntity.class::cast)
                .toList();

        for (LivingEntity enemy : enemies) {
            if (properties.getDamagedEntities().contains(enemy.getUniqueId())) {
                continue; // Already damaged
            }

            final DamageEvent event = UtilDamage.doDamage(new DamageEvent(
                    enemy,
                    player,
                    player,
                    new ItemAbilityDamageCause(this).withBukkitCause(EntityDamageEvent.DamageCause.POISON),
                    damage,
                    getName()
            ));
            if (!event.isCancelled()) {
                // Effect
                effectManager.addEffect(enemy, player, EffectTypes.POISON, getName(), poisonAmplifier, (long) (poisonSeconds * 1000L));

                // Velocity
                VelocityData data = new VelocityData(player.getLocation().getDirection(),
                        1.2,
                        false,
                        0,
                        0.2,
                        1.0,
                        true);
                UtilVelocity.velocity(enemy, player, data);
                properties.getDamagedEntities().add(enemy.getUniqueId());
            }
        }
    }

    private void dash(Player player, Properties properties) {
        // Give step height to allow for smoother gameplay
        final AttributeInstance attribute = Objects.requireNonNull(player.getAttribute(Attribute.STEP_HEIGHT));
        if (attribute.getModifier(getKey()) == null) {
            attribute.addTransientModifier(new AttributeModifier(getKey(), 0.4, AttributeModifier.Operation.ADD_NUMBER));
        }

        // Apply velocity
        VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), speed, false, 0, 0, 1.0, true);
        UtilVelocity.velocity(player, null, velocityData);

        // Particle cues
        final List<Location> points = new ArrayList<>();
        if (properties.getLastLocation() != null) {
            final Location[] calc = VectorLine.withStepSize(properties.getLastLocation(), player.getLocation(), 0.2).toLocations();
            points.addAll(List.of(calc));
        } else {
            points.add(player.getLocation());
        }

        for (Location point : points) {
            Particle.DUST_PILLAR.builder()
                    .data(Material.VINE.createBlockData())
                    .count(10)
                    .extra(0.01)
                    .location(point)
                    .receivers(60)
                    .spawn();
        }

        new SoundEffect(Sound.BLOCK_WET_GRASS_BREAK, 0.8f, 1f).play(player.getLocation());
    }

    private void stop(Player player) {
        // Remove step height modifier
        final AttributeInstance attribute = Objects.requireNonNull(player.getAttribute(Attribute.STEP_HEIGHT));
        attribute.removeModifier(getKey());

        // Stop their velocity
        player.setVelocity(new Vector());

        // Particle cues
        new SoundEffect(Sound.BLOCK_GRASS_PLACE, 0.8f, 1f).play(player.getLocation());
        new SoundEffect(Sound.BLOCK_FIRE_EXTINGUISH, 1.8f, 0.2f).play(player.getLocation());
    }

    private void tick() {
        final Iterator<Map.Entry<Player, Properties>> iterator = active.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, Properties> cur = iterator.next();
            final Player player = cur.getKey();
            final Properties properties = cur.getValue();

            // Offline players
            if (player == null || !player.isValid()) {
                iterator.remove();
                continue;
            }

            // Cancel their dash when they stop clicking or the time expires
            final Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isHoldingRightClick() || UtilTime.elapsed(properties.getStartTime(), (long) (duration * 1000))) {
                iterator.remove();
                cooldownManager.use(player, getName(), cooldown, true);
                stop(player);
            }
        }
    }

    @Data
    private static class Properties {
        List<UUID> damagedEntities = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        Location lastLocation = null;
    }
}
