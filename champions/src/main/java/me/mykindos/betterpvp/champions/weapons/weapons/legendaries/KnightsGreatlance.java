package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.weapons.types.ChannelWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.Cooldown;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class KnightsGreatlance extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String ATTACK_NAME = "Knight's Greatlance Charge";

    private final WeakHashMap<Player, LanceData> active = new WeakHashMap<>();
    private final CooldownManager cooldownManager;
    private final ClientManager clientManager;
    private final EffectManager effectManager;

    @Inject
    @Config(path = "weapons.knights-greatlance.energy-per-tick", defaultValue = "1.0", configName = "weapons/legendaries")
    private double energyPerTick;

    @Inject
    @Config(path = "weapons.knights-greatlance.initial-energy-cost", defaultValue = "10.0", configName = "weapons/legendaries")
    private double initialEnergyCost;

    @Inject
    @Config(path = "weapons.knights-greatlance.base-damage", defaultValue = "8.0", configName = "weapons/legendaries")
    private double baseDamage;

    @Inject
    @Config(path = "weapons.knights-greatlance.charge-cooldown", defaultValue = "5.0", configName = "weapons/legendaries")
    private double attackCooldown;

    @Inject
    @Config(path = "weapons.knights-greatlance.max-charge-ticks", defaultValue = "60", configName = "weapons/legendaries")
    private int maxChargeTicks;

    @Inject
    @Config(path = "weapons.knights-greatlance.charge-velocity", defaultValue = "1.5", configName = "weapons/legendaries")
    private double chargeVelocity;

    private final EnergyHandler energyHandler;

    private final PermanentComponent actionBar = new PermanentComponent(gmr -> {
        if (!gmr.isOnline() || !active.containsKey(gmr.getPlayer())) {
            return null;
        }

        final double charge = active.get(gmr.getPlayer()).getTicksCharged();
        final float percent = (float) (charge / maxChargeTicks);
        return new ProgressBar(percent, 24).build();
    });

    @Inject
    public KnightsGreatlance(final CooldownManager cooldownManager, final ClientManager clientManager, final EffectManager effectManager, EnergyHandler energyHandler) {
        super("knights_greatlance");
        this.cooldownManager = cooldownManager;
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.energyHandler = energyHandler;
    }

    @Override
    public List<Component> getLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Relic of a bygone age.", NamedTextColor.WHITE));
        lore.add(Component.text("Emblazoned with cryptic runes, this", NamedTextColor.WHITE));
        lore.add(Component.text("Lance bears the marks of its ancient master.", NamedTextColor.WHITE));
        lore.add(Component.text("You feel him with you always:", NamedTextColor.WHITE));
        lore.add(Component.text("Heed his warnings and stave off the darkness.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Charge"));
        return lore;
    }

    @Override
    public void loadWeapon(BPVPItem item) {
        super.loadWeapon(item);

    }

    @Override
    public void activate(Player player) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        if (!active.containsKey(player)) {
            gamer.getActionBar().add(250, actionBar);
        }
        active.putIfAbsent(player, new LanceData(getMidpoint(player), gamer, 0));
        this.effectManager.addEffect(player, EffectType.NO_JUMP, -50);
    }

    private void deactivate(Player player, LanceData data) {
        this.effectManager.removeEffect(player, EffectType.NO_JUMP);
        data.getGamer().getActionBar().remove(actionBar);
    }

    private Location getMidpoint(Player player) {
        final Location location = player.getLocation();
        final double height = player.getHeight();
        return location.add(0.0, height / 2, 0.0);
    }

    @UpdateEvent
    public void doLance() {
        final Iterator<Map.Entry<Player, LanceData>> iterator = active.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, LanceData> cur = iterator.next();
            final Player player = cur.getKey();
            final LanceData data = cur.getValue();
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (player.getInventory().getItemInMainHand().getType() != getMaterial()) {
                iterator.remove();
                deactivate(player, data);
                continue;
            }

            final Gamer gamer = data.getGamer();
            if (!gamer.isHoldingRightClick()) {
                iterator.remove();
                deactivate(player, data);
                continue;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                iterator.remove();
                deactivate(player, data);
                continue;
            }

            if (!UtilBlock.isGrounded(player)) {
                data.setTicksCharged(0); // Reset charge
                continue;
            }

            if (!canUse(player)) {
                iterator.remove();
                deactivate(player, data);
                continue;
            }

            if (!energyHandler.use(player, "Knight's Greatlance", energyPerTick, true)) {
                iterator.remove();
                deactivate(player, data);
                return;
            }

            // Get all enemies that collide with the player from the last location to the new location
            final Location newLocation = getMidpoint(player);
            final Optional<LivingEntity> hit = trace(player, data.getLastLocation(), newLocation);
            final int charge = data.getTicksCharged();
            final double percentage = (float) charge / maxChargeTicks;
            if (hit.isPresent()) {
                final LivingEntity hitEnt = hit.get();
                final double damage = 2 + (10 * percentage);

                // Remove data
                deactivate(player, data);

                // Sound
                new SoundEffect(Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.5f).play(player.getLocation());

                // Damage
                UtilDamage.doCustomDamage(new CustomDamageEvent(hitEnt,
                        player,
                        null,
                        EntityDamageEvent.DamageCause.CUSTOM,
                        damage,
                        false,
                        ATTACK_NAME));

                // Velocity
                final Vector knockback = player.getLocation().getDirection();
                UtilVelocity.velocity(hitEnt, knockback, 2.6, true, 0, 0.2, 1.4, true);

                // Cooldown
                this.cooldownManager.use(player,
                        ATTACK_NAME,
                        attackCooldown,
                        true,
                        true,
                        false,
                        gmr -> isHoldingWeapon(Objects.requireNonNull(gmr.getPlayer())));
                continue;
            } else if (charge < maxChargeTicks) {
                // Update data
                data.setTicksCharged(charge + 1);
            }


            // Move
            data.setLastLocation(newLocation);
            Vector direction = player.getLocation().getDirection().multiply(chargeVelocity);
            direction.setY(0); // Make them stick to the ground
            player.setVelocity(direction);

            // Cues
            new ParticleBuilder(Particle.CRIT)
                    .location(player.getLocation())
                    .offset(0.5, 0.5, 0.5)
                    .count(5)
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }
    }

    private Optional<LivingEntity> trace(@NotNull Player player, @NotNull Location lastLocation, @NotNull Location location) {
        Preconditions.checkNotNull(player, "Player cannot be null");
        Preconditions.checkNotNull(lastLocation, "Last location cannot be null");
        Preconditions.checkNotNull(location, "Location cannot be null");
        Preconditions.checkArgument(location.getWorld() == lastLocation.getWorld(), "Locations must be in the same world");

        final Vector direction = location.getDirection();
        final RayTraceResult result = lastLocation.getWorld().rayTraceEntities(lastLocation, direction, direction.length(), 0.6, entity -> {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) {
                return false;
            }

            if (!(entity instanceof Player other)) {
                return true;
            }

            return UtilPlayer.getRelation(player, other) != EntityProperty.FRIENDLY;
        });

        if (result == null) {
            return Optional.empty();
        }

        return Optional.ofNullable((LivingEntity) result.getHitEntity());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        final LanceData remove = active.remove(event.getPlayer());
        if (remove != null) {
            deactivate(event.getPlayer(), remove);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (isHoldingWeapon(damager)) {
            event.setDamage(baseDamage);
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.simpleMessage(player, "Knight's Greatlance", "You cannot use this weapon while in water!");
            return false;
        }
        final Cooldown cooldown = this.cooldownManager.getAbilityRecharge(player, ATTACK_NAME);
        return cooldown == null || cooldown.getRemaining() <= 0;
    }

    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class LanceData {
        private @NotNull Location lastLocation;
        private @NotNull Gamer gamer;
        private int ticksCharged;
    }

}
