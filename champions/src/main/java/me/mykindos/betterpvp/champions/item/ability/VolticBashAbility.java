package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbilityDamageCause;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.WeakHashMap;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class VolticBashAbility extends ItemAbility {

    private double velocity;
    private int maxChargeTicks;
    private double energyOnCollide;
    private double chargeDamage;
    private double energyPerTick;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final ClientManager clientManager;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;
    @EqualsAndHashCode.Exclude
    private final EnergyHandler energyHandler;
    @EqualsAndHashCode.Exclude
    private final WeakHashMap<Player, AegisData> cache = new WeakHashMap<>();

    @Inject
    private VolticBashAbility(Champions champions, ClientManager clientManager, EffectManager effectManager, EnergyHandler energyHandler) {
        super(new NamespacedKey(champions, "voltic_bash"),
                "Voltic Bash",
                "Charge up and dash forward, dealing damage to entities in your path. Higher charge increases damage and velocity.",
                TriggerTypes.HOLD_RIGHT_CLICK);
        this.champions = champions;
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.energyHandler = energyHandler;
        
        // Default values, will be overridden by config
        this.velocity = 0.8;
        this.maxChargeTicks = 60;
        this.energyOnCollide = 25.0;
        this.chargeDamage = 7.0;
        this.energyPerTick = 1.0;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        // Check if player is in liquid
        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.simpleMessage(player, "Thunderclap Aegis", "You cannot use <green>Voltic Bash <gray>while in water.");
            return false;
        }
        
        // Initialize or continue the charge
        processCharge(player);
        return true;
    }
    
    /**
     * Process the charge for a player
     */
    private void processCharge(Player player) {
        // Get or create data
        AegisData data = cache.computeIfAbsent(player, key -> {
            final Gamer gamer = clientManager.search().online(player).getGamer();
            return new AegisData(UtilPlayer.getMidpoint(player), gamer, 0, System.currentTimeMillis());
        });
        
        // Check if grounded
        if (!UtilBlock.isGrounded(player)) {
            data.setTicksCharged(0); // Reset charge
            return;
        }
        
        // Check energy
        if (!energyHandler.use(player, getName(), energyPerTick, true)) {
            return;
        }
        
        data.getLastHit().entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 500L);
        
        // Get all enemies that collide with the player from the last location to the new location
        final Location newLocation = UtilPlayer.getMidpoint(player);
        final var collisions = UtilEntity.interpolateMultiCollision(data.getLastLocation(),
                        newLocation,
                        0.6f,
                        ent -> UtilEntity.IS_ENEMY.test(player, ent))
                .stream()
                .flatMap(MultiRayTraceResult::stream)
                .map(RayTraceResult::getHitEntity)
                .filter(LivingEntity.class::isInstance)
                .filter(ent -> !data.getLastHit().containsKey(ent))
                .map(LivingEntity.class::cast)
                .toList();
        
        final int charge = data.getTicksCharged();
        if (!collisions.isEmpty()) {
            final double percentage = getChargePercentage(charge);
            this.energyHandler.degenerateEnergy(player, this.energyOnCollide / 100);
            for (LivingEntity hit : collisions) {
                collide(player, hit, percentage, data);
            }
        } else if (charge < maxChargeTicks) {
            // Update data
            data.setTicksCharged(charge + 1);
        }
        
        // Move
        data.setLastLocation(newLocation);
        this.effectManager.addEffect(player, EffectTypes.NO_JUMP, getName(), 1, 100);
        final double velocity = Math.min(getVelocity() + (this.velocity * getChargePercentage(charge)), this.velocity);
        final Vector direction = player.getLocation().getDirection().setY(0).normalize();
        VelocityData velocityData = new VelocityData(direction, velocity, true, -0.1, 0.0, -0.1, false);
        UtilVelocity.velocity(player, null, velocityData);

        // Durability
        if (Bukkit.getCurrentTick() % 20 == 0) {
            UtilItem.damageItem(player, player.getActiveItem(), 1);
        }
        
        new SoundEffect(Sound.BLOCK_BEEHIVE_WORK, 0f, 1.5f).play(player.getLocation());
        new SoundEffect(Sound.BLOCK_HANGING_SIGN_WAXED_INTERACT_FAIL, 0f, 1.0f).play(player.getLocation());
        
        // Cues
        new ParticleBuilder(Particle.ELECTRIC_SPARK)
                .location(player.getLocation())
                .offset(0.7, 0.7, 0.7)
                .count(5)
                .extra(0)
                .receivers(60)
                .spawn();
    }
    
    /**
     * Calculate charge percentage
     */
    private float getChargePercentage(float ticks) {
        return ticks / maxChargeTicks;
    }
    
    /**
     * Handle collision with entity
     */
    private void collide(Player caster, LivingEntity hit, double charge, AegisData data) {
        // Damage
        final DamageEvent event = new DamageEvent(hit,
                caster,
                null,
                new ItemAbilityDamageCause(this),
                chargeDamage * charge,
                getName());
        event.setForceDamageDelay(0);
        UtilDamage.doDamage(event);
        data.getLastHit().put(hit, System.currentTimeMillis());
        if (event.isCancelled()) {
            return;
        }
        
        // Sound
        new SoundEffect(Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2f, 1.5f).play(caster.getLocation());
        
        // Velocity
        final Vector vec = caster.getLocation().getDirection();
        VelocityData velocityData = new VelocityData(vec, 1.5 * charge + 1.1, true, 0, 0.2, 1.4, true, false);
        UtilVelocity.velocity(hit, caster, velocityData);
    }
    
    @Getter
    @Setter
    @AllArgsConstructor
    private static class AegisData {
        private final WeakHashMap<LivingEntity, Long> lastHit = new WeakHashMap<>();
        private @NotNull Location lastLocation;
        private @NotNull Gamer gamer;
        private int ticksCharged;
        private long lastPulse;
    }
} 