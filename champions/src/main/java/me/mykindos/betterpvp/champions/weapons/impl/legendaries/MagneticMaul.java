package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class MagneticMaul extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String ABILITY_NAME = "Magnetism";
    private double pullRange;
    private double pullFov;
    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;

    @Inject
    public MagneticMaul(Champions champions, EnergyHandler energyHandler, ClientManager clientManager) {
        super(champions, "magnetic_maul");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("For centuries, warlords used", NamedTextColor.WHITE));
        lore.add(Component.text("this hammer to control their", NamedTextColor.WHITE));
        lore.add(Component.text("subjects. This brutal weapon", NamedTextColor.WHITE));
        lore.add(Component.text("allows you to pull your enemies", NamedTextColor.WHITE));
        lore.add(Component.text("towards you with magnetic force.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>" + ABILITY_NAME));
        return lore;
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

    @UpdateEvent (priority = 99)
    public void doMaul() {
        if (!enabled) {
           return;
        }
        active.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return true;

            if (player.getInventory().getItemInMainHand().getType() != getMaterial()) {
                return true;
            }

            final Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                return true;
            }

            if (!canUse(player)) {
                return true;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                return true;
            }

            if (!energyHandler.use(player, ABILITY_NAME, energyPerTick, true)) {
                return true;
            }

            pull(player);
            playCone(player);
            new SoundEffect(Sound.BLOCK_BEACON_DEACTIVATE, 0F, 1F).play(player.getLocation());
            return false;
        });

    }

    private void playCone(Player wielder) {
        final float particleStep = 0.25f;
        final double particlePoints = pullRange / particleStep;

        // Calculate the amplitude, in blocks, based on the DEFAULT_FOV
        double amplitude = Math.tan(Math.toRadians(pullFov / 2)) * pullRange;

        final World world = wielder.getWorld();
        final Location origin = wielder.getEyeLocation();
        final Vector direction = wielder.getLocation().getDirection().multiply(particleStep); // 0.25 block step size for particles
        final double pitch = Math.toRadians(wielder.getLocation().getPitch() + 90);
        final double yaw = Math.toRadians(-wielder.getLocation().getYaw());

        // Create both rotating vectors
        final double time = ((System.currentTimeMillis() / 10d) % 360);
        for (int i = 0; i < particlePoints; i ++) {
            float distance = i * particleStep;
            if (distance < 1) continue; // Skip first 4 particles (too close to player)

            final Location point = origin.clone().add(direction).toLocation(world);
            final double angle = Math.toRadians(time + (i * 10));
            final double pointAmplitude = amplitude * (i / particlePoints);
            double x1 = Math.cos(angle) * pointAmplitude;
            double z1 = Math.sin(angle) * pointAmplitude;

            // First spiral
            Vector spiralPoint = new Vector(x1, 0, z1);
            spiralPoint.rotateAroundX(pitch);
            spiralPoint.rotateAroundY(yaw);
            spiralPoint.add(point.toVector());
            final Location spiralLoc = spiralPoint.toLocation(world);
            new ParticleBuilder(Particle.ENCHANTED_HIT).location(spiralLoc).extra(0).receivers(60).spawn();

            // Second spiral
            Vector spiralPoint2 = new Vector(-x1, 0, -z1);
            spiralPoint2.rotateAroundX(pitch);
            spiralPoint2.rotateAroundY(yaw);
            spiralPoint2.add(point.toVector());
            final Location spiralLoc2 = spiralPoint2.toLocation(world);
            new ParticleBuilder(Particle.ENCHANTED_HIT).location(spiralLoc2).extra(0).receivers(60).spawn();

            origin.add(direction); // Move origin forward
        }

    }

    private void playPullLine(Player wielder, Entity entity) {
        final Location origin = wielder.getEyeLocation().add(wielder.getLocation().getDirection());
        final Location target = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        final VectorLine line = VectorLine.withStepSize(origin, target, 0.5);
        for (Location location : line.toLocations()) {
            new ParticleBuilder(Particle.ASH).location(location).extra(0).receivers(60).spawn();
        }
    }

    private void pull(Player wielder) {
        final Location pullLocation = wielder.getEyeLocation();

        final List<KeyValue<LivingEntity, EntityProperty>> nearby = UtilEntity.getNearbyEntities(wielder, pullLocation, pullRange, EntityProperty.ALL);
        for (KeyValue<LivingEntity, EntityProperty> entry : nearby) {
            final LivingEntity entity = entry.get();
            if (entity == wielder) {
                continue; // Skip self
            }

            if (!wielder.hasLineOfSight(entity.getLocation()) && !wielder.hasLineOfSight(entity.getEyeLocation())) {
                continue; // Skip entities not in line of sight
            }

            // Get angle from player to entity
            final double angle = Math.toDegrees(wielder.getLocation().getDirection()
                    .angle(entity.getLocation().toVector().subtract(wielder.getLocation().toVector())));
            if (angle > pullFov / 2) {
                continue; // Skip entities not in front of us
            }

            final Vector trajectory = pullLocation.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.3);
            VelocityData velocityData = new VelocityData(trajectory, 0.3, false, 0, 0, 1, true);
            UtilVelocity.velocity(entity, wielder, velocityData);
            playPullLine(wielder, entity);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreDamageEvent event) {
        if (!enabled) {
            return;
        }

        DamageEvent de = event.getDamageEvent();

        if (de.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(de.getDamager() instanceof Player damager)) return;
        if (isHoldingWeapon(damager)) {
            de.setDamage(baseDamage);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKB(CustomKnockbackEvent event) {
        if (!enabled) {
            return;
        }
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCustomDamageEvent().getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return; // Only apply to melee attacks
        }

        if (isHoldingWeapon(damager)) {
            event.setCanBypassMinimum(true);
            event.setMultiplier(-1);
        }
    }

    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

    @Override
    public void loadWeaponConfig() {
        pullRange = getConfig("pullRange", 10.0, Double.class);
        pullFov = getConfig("pullFov", 80.3, Double.class);
    }
}
