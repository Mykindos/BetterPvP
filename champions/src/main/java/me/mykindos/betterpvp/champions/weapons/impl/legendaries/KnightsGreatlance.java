package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.cooldowns.Cooldown;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
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
import org.bukkit.inventory.ItemStack;
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
    private double attackCooldown;
    private int maxChargeTicks;
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
    public KnightsGreatlance(Champions champions, final CooldownManager cooldownManager, final ClientManager clientManager, final EffectManager effectManager, EnergyHandler energyHandler) {
        super(champions, "knights_greatlance");
        this.cooldownManager = cooldownManager;
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.energyHandler = energyHandler;
    }

    @Override
    public List<Component> getLore(ItemStack item) {
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
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

    }

    @Override
    public void activate(Player player) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        if (!active.containsKey(player)) {
            gamer.getActionBar().add(250, actionBar);
        }
        active.putIfAbsent(player, new LanceData(UtilPlayer.getMidpoint(player), gamer, 0));
    }

    private void deactivate(Player player, LanceData data) {
        data.getGamer().getActionBar().remove(actionBar);
    }

    @UpdateEvent
    public void doLance() {
        if (!enabled) {
            return;
        }
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
            final Location newLocation = UtilPlayer.getMidpoint(player);
            final Optional<LivingEntity> hit = UtilEntity.interpolateCollision(data.getLastLocation(),
                            newLocation,
                            0.6f,
                            ent -> UtilEntity.IS_ENEMY.test(player, ent))
                    .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

            final int charge = data.getTicksCharged();
            final double percentage = (float) charge / maxChargeTicks;
            if (hit.isPresent()) {
                final LivingEntity hitEnt = hit.get();
                final double damage = 2 + (10 * percentage);

                // Damage
                CustomDamageEvent customDamageEvent = UtilDamage.doCustomDamage(new CustomDamageEvent(hitEnt,
                        player,
                        null,
                        EntityDamageEvent.DamageCause.CUSTOM,
                        damage,
                        false,
                        ATTACK_NAME));
                if (customDamageEvent == null || customDamageEvent.isCancelled()) continue;

                // Remove data
                deactivate(player, data);

                // Sound
                new SoundEffect(Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.5f).play(player.getLocation());

                // Velocity
                final Vector vec = player.getLocation().getDirection();
                VelocityData velocityData = new VelocityData(vec, 2.6, true, 0, 0.2, 1.4, true);
                UtilVelocity.velocity(hitEnt, player, velocityData);

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
            this.effectManager.addEffect(player, EffectType.NO_JUMP, 100);
            VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), chargeVelocity, true, 0, 0.0, 0.0, false);
            UtilVelocity.velocity(player, null, velocityData);

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

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        final LanceData remove = active.remove(event.getPlayer());
        if (remove != null) {
            deactivate(event.getPlayer(), remove);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (!enabled) {
            return;
        }
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

    @Override
    public void loadWeaponConfig() {
        attackCooldown = getConfig("attackCooldown", 5.0, Double.class);
        maxChargeTicks = getConfig("maxChargeTicks", 60, Integer.class);
        chargeVelocity = getConfig("chargeVelocity", 1.5, Double.class);
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
