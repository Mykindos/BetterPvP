package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

@Singleton
@BPvPListener
public class ThunderclapAegis extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String ABILITY_NAME = "Voltic Bash";

    private final WeakHashMap<Player, AegisData> cache = new WeakHashMap<>();
    private final ClientManager clientManager;
    private final EffectManager effectManager;
    private final EnergyService energyService;
    private int maxChargeTicks;
    private double baseVelocity;
    private double chargeDamage;
    private double maxVelocity;
    private double energyOnCollide;

    private final PermanentComponent actionBar = new PermanentComponent(gmr -> {

        AegisData aegisData = cache.get(gmr.getPlayer());
        if (!gmr.isOnline() || aegisData == null) {
            return null;
        }


        final double charge = aegisData.getTicksCharged();
        final float percent = (float) (charge / maxChargeTicks);
        return new ProgressBar(percent, 24)
                .withProgressColor(NamedTextColor.GOLD)
                .withRemainingColor(NamedTextColor.WHITE)
                .build();
    });

    @Inject
    public ThunderclapAegis(Champions champions, final ClientManager clientManager, final EffectManager effectManager, EnergyService energyService) {
        super(champions, "thunderclap_aegis");
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.energyService = energyService;
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Forged amidst celestial tempests", NamedTextColor.WHITE));
        lore.add(Component.text("by the divine blacksmiths, this", NamedTextColor.WHITE));
        lore.add(Component.text("shield channels the wrath of the", NamedTextColor.WHITE));
        lore.add(Component.text("gods. Its charge, a testament to", NamedTextColor.WHITE));
        lore.add(Component.text("their fury, rends adversaries with", NamedTextColor.WHITE));
        lore.add(Component.text("divine energy.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>%s", ABILITY_NAME));
        return lore;
    }

    @Override
    public void activate(Player player) {
        cache.computeIfAbsent(player, key -> {
            final Gamer gamer = clientManager.search().online(player).getGamer();
            gamer.getActionBar().add(250, actionBar);
            return new AegisData(UtilPlayer.getMidpoint(player), gamer, 0, System.currentTimeMillis());
        });
    }

    private void deactivate(AegisData data) {
        data.getGamer().getActionBar().remove(actionBar);
    }

    @Override
    public boolean useShield(Player player) {
        return true;
    }

    @Override
    public boolean hasDurability() {
        return false;
    }

    private void collide(Player caster, LivingEntity hit, double charge, AegisData data) {
        // Damage
        final CustomDamageEvent event = new CustomDamageEvent(hit,
                caster,
                null,
                EntityDamageEvent.DamageCause.CUSTOM,
                chargeDamage * charge,
                false,
                ABILITY_NAME);
        event.setForceDamageDelay(0);
        UtilDamage.doCustomDamage(event);
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

    @UpdateEvent(priority = 100)
    public void doThunderclapAegis() {
        if (!enabled) {
            return;
        }

        final Iterator<Map.Entry<Player, AegisData>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, AegisData> cur = iterator.next();
            final Player player = cur.getKey();
            final AegisData data = cur.getValue();
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (!isHoldingWeapon(player)) {
                iterator.remove();
                deactivate(data);
                activeUsageNotifications.remove(player.getUniqueId());
                continue;
            }

            final Gamer gamer = data.getGamer();
            if (!gamer.isHoldingRightClick()) {
                iterator.remove();
                deactivate(data);
                activeUsageNotifications.remove(player.getUniqueId());
                continue;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                iterator.remove();
                deactivate(data);
                activeUsageNotifications.remove(player.getUniqueId());
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                continue;
            }

            if (!UtilBlock.isGrounded(player)) {
                data.setTicksCharged(0); // Reset charge
                continue;
            }

            if (!canUse(player)) {
                deactivate(data);
                continue;
            }

            if (!energyService.use(player, ABILITY_NAME, energyPerTick, true)) {
                iterator.remove();
                deactivate(data);
                activeUsageNotifications.remove(player.getUniqueId());
                return;
            }

            data.getLastHit().entrySet().removeIf(entry -> UtilTime.elapsed(entry.getValue(), 500L));

            // Get all enemies that collide with the player from the last location to the new location
            final Location newLocation = UtilPlayer.getMidpoint(player);
            final List<LivingEntity> collisions = UtilEntity.interpolateMultiCollision(data.getLastLocation(),
                            newLocation,
                            0.6f,
                            ent -> {
                                if (ent instanceof LivingEntity livingEntity) {
                                    EntityCanHurtEntityEvent entityCanHurtEntityEvent = UtilServer.callEvent(new EntityCanHurtEntityEvent(player, livingEntity));
                                    return entityCanHurtEntityEvent.isAllowed();
                                }

                                return false;
                            })
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
                this.energyService.degenerateEnergy(player, this.energyOnCollide / 100, EnergyEvent.CAUSE.USE);
                for (LivingEntity hit : collisions) {
                    collide(player, hit, percentage, data);
                }

                continue;
            } else if (charge < maxChargeTicks) {
                // Update data
                data.setTicksCharged(charge + 1);
            }

            // Move
            data.setLastLocation(newLocation);
            this.effectManager.addEffect(player, EffectTypes.NO_JUMP, ABILITY_NAME, 1, 100);
            final double velocity = Math.min(baseVelocity + (maxVelocity * getChargePercentage(charge)), maxVelocity);
            final Vector direction = player.getLocation().getDirection().setY(0).normalize();
            VelocityData velocityData = new VelocityData(direction, velocity, true, -0.1, 0.0, -0.1, false);
            UtilVelocity.velocity(player, null, velocityData);

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
    }

    private float getChargePercentage(float ticks) {
        return ticks / maxChargeTicks;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        final AegisData remove = cache.remove(event.getPlayer());
        if (remove != null) {
            deactivate(remove);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreDamageEvent event) {
        if (!enabled) {
            return;
        }

        DamageEvent cde = event.getDamageEvent();
        if (cde.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (isHoldingWeapon(damager)) {
            cde.setDamage(baseDamage);
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInLiquid(player)) {
            if (!activeUsageNotifications.contains(player.getUniqueId())) {
                UtilMessage.simpleMessage(player, getSimpleName(), String.format("You cannot use <green>%s <gray>while in water", ABILITY_NAME));
                activeUsageNotifications.add(player.getUniqueId());
            }
            return false;
        }
        activeUsageNotifications.remove(player.getUniqueId());
        return true;
    }

    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

    @Override
    public void loadWeaponConfig() {
        baseVelocity = getConfig("baseVelocity", 0.5, Double.class);
        maxVelocity = getConfig("maxVelocity", 0.8, Double.class);
        maxChargeTicks = getConfig("maxChargeTicks", 60, Integer.class);
        energyOnCollide = getConfig("energyOnCollide", 25.0, Double.class);
        chargeDamage = getConfig("chargeDamage", 7.0, Double.class);
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