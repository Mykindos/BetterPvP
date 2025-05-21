package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Singleton
@BPvPListener
public class WindBlade extends Weapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String ABILITY_NAME = "Wind Dash";
    private static final String ABILITY_NAME_2 = "Wind Slash";

    private final EnergyService energyService;
    private final ChampionsManager championsManager;
    private final CooldownManager cooldownManager;
    private final Champions champions;

    // Config
    private double slashHitboxSize;
    private double slashDamage;
    private double slashVelocity;
    private double dashVelocity;
    private double slashEnergyRefundPercent;
    private double dashImpactVelocity;
    private double slashCooldown;
    private double slashSpeed;
    private int dashParticleTicks;
    private int slashEnergyCost;
    private int dashEnergyCost;
    private int slashAliveMillis;

    // Data
    private final Map<Player, Dash> dashSet = new WeakHashMap<>();
    private final Set<Slash> slashSet = new HashSet<>();

    @Inject
    public WindBlade(Champions champions, EnergyService energyService, ChampionsManager championsManager, CooldownManager cooldownManager) {
        super(champions, "wind_blade");
        this.champions = champions;
        this.energyService = energyService;
        this.cooldownManager = cooldownManager;
        this.championsManager = championsManager;
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Long ago, a race of cloud dwellers", NamedTextColor.WHITE));
        lore.add(Component.text("terrorized the skies. A remnant of", NamedTextColor.WHITE));
        lore.add(Component.text("their tyranny, this airy blade is", NamedTextColor.WHITE));
        lore.add(Component.text("the last surviving memoriam from", NamedTextColor.WHITE));
        lore.add(Component.text("their final battle against the Titans.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>%s<green>", ABILITY_NAME));
        lore.add(UtilMessage.deserialize("<yellow>Left-Click <white>to use <green>" + ABILITY_NAME_2 + "<green>"));
        return lore;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isLeftClick() || !isHoldingWeapon(player)) {
            return;
        }

        if (!this.cooldownManager.use(player, ABILITY_NAME_2, slashCooldown, false, true, false, gmr -> isHoldingWeapon(player), 900)) {
            return;
        }

        if (!energyService.use(player, ABILITY_NAME_2, slashEnergyCost, true)) {
            return;
        }

        // SFX
        new SoundEffect(Sound.ENTITY_PHANTOM_FLAP, 1.2F, 2.0F).play(player.getLocation());

        // Fire
        Location origin = player.getEyeLocation();
        final double rotation = Math.toRadians(30);

        Vector mainDirection = origin.getDirection().normalize();
        Vector leftDirection = mainDirection.clone().rotateAroundY(rotation).normalize();
        Vector rightDirection = mainDirection.clone().rotateAroundY(-rotation).normalize();

        Slash mainSlash = new Slash(player, origin);
        mainSlash.redirect(mainDirection.multiply(slashSpeed));
        Slash leftSlash = new Slash(player, origin);
        leftSlash.redirect(leftDirection.multiply(slashSpeed));
        Slash rightSlash = new Slash(player, origin);
        rightSlash.redirect(rightDirection.multiply(slashSpeed));

        slashSet.add(mainSlash);
        slashSet.add(leftSlash);
        slashSet.add(rightSlash);
    }

    @Override
    public void activate(Player player) {
        if (!championsManager.getEnergy().use(player, getSimpleName(), dashEnergyCost, true)) {
            return;
        }

        dashSet.put(player, new Dash());

        // Velocity
        Vector vec = player.getLocation().getDirection().normalize().multiply(dashVelocity);
        VelocityData velocityData = new VelocityData(vec, dashVelocity, false, 0.0D, 0.4D, 1D, true);
        UtilVelocity.velocity(player, player, velocityData);

        // SFX
        new SoundEffect(Sound.ITEM_TRIDENT_THROW, 0.5F, 2.0F).play(player.getLocation());

        // VFX
        UtilMessage.message(player, "Wind Blade", "You used <alt>" + ABILITY_NAME + "</alt>.");
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
    }

    @UpdateEvent
    public void doSlash() {
        final Iterator<Slash> slashIterator = slashSet.iterator();
        while (slashIterator.hasNext()) {
            final Slash slash = slashIterator.next();
            final Player player = slash.getCaster();

            if (!player.isOnline() || slash.isMarkForRemoval() || slash.isExpired()) {
                slashIterator.remove(); // Remove offline players or done lines
                continue;
            }

            slash.tick();
        }
    }

    @UpdateEvent
    public void doDash() {
        Iterator<Map.Entry<Player, Dash>> dashIterator = dashSet.entrySet().iterator();
        while (dashIterator.hasNext()) {
            Map.Entry<Player, Dash> entry = dashIterator.next();
            Dash dash = entry.getValue();
            Player player = entry.getKey();
            if (player.isDead() || !player.isOnline()) {
                dashIterator.remove(); // Remove offline or dead players
                continue;
            }

            if (UtilBlock.isGrounded(player) && UtilTime.elapsed(dash.getTime(), 750L)) {
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

            // Velocity
            Vector upwardVelocity = new Vector(0, 1, 0).multiply(dashImpactVelocity);
            UtilVelocity.velocity(target, player, new VelocityData(upwardVelocity, 1.0, 0.0, 10.0, false));

            // SFX & VFX
            new SoundEffect(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 2).play(target.getLocation());
            new SoundEffect(Sound.ENTITY_PUFFER_FISH_STING, 0.8F, 1.5F).play(target.getLocation());
            UtilMessage.message(player, "Wind Blade", "You hit <alt2>" + target.getName() + "</alt2> with <alt>" + ABILITY_NAME + "</alt>.");
            UtilMessage.message(target, "Wind Blade", "<alt2>" + player.getName() + "</alt2> hit you with <alt>" + ABILITY_NAME + "</alt>.");
        }
    }

    // set base damage to 0
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!enabled || cde.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !(cde.getDamager() instanceof Player damager)) {
            return;
        }

        if (isHoldingWeapon(damager)) {
            cde.setDamage(baseDamage);
            cde.setRawDamage(baseDamage);
        }
    }

    // cancel fall damage
    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!enabled || !(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (isHoldingWeapon(player)) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.simpleMessage(player, getSimpleName(), String.format("You cannot use <green>%s <gray>while in liquid", ABILITY_NAME));
            return false;
        }
        return true;
    }

    @Override
    public void loadWeaponConfig() {
        dashVelocity = getConfig("dashVelocity", 1.2, Double.class);
        dashParticleTicks = getConfig("dashParticleTicks", 2, Integer.class);
        dashEnergyCost = getConfig("dashEnergyCost", 40, Integer.class);
        dashImpactVelocity = getConfig("dashImpactVelocity", 1.0, Double.class);
        slashCooldown = getConfig("slashCooldown", 2.5, Double.class);
        slashHitboxSize = getConfig("slashHitboxSize", 0.6, Double.class);
        slashEnergyCost = getConfig("slashEnergyCost", 0, Integer.class);
        slashDamage = getConfig("slashDamage", 5.0, Double.class);
        slashEnergyRefundPercent = getConfig("slashEnergyRefundPercent", 0.2, Double.class);
        slashVelocity = getConfig("slashVelocity", 0.5, Double.class);
        slashAliveMillis = getConfig("slashAliveMillis", 1000, Integer.class);
        slashSpeed = getConfig("slashSpeed", 1.5, Double.class);
    }

    @Data
    private static class Dash {
        private final long time = System.currentTimeMillis();
        private final Set<LivingEntity> hitTargets = new HashSet<>();
    }

    private class Slash extends Projectile {

        private final Set<LivingEntity> hitTargets = new HashSet<>();

        private Slash(@Nullable Player caster, Location location) {
            super(caster, slashHitboxSize, location, slashAliveMillis);
        }

        @Override
        protected void onTick() {
            final Collection<Player> receivers = location.getNearbyPlayers(60);
            for (Location point : interpolateLine()) {
                // Play travel particles
                Particle.POOF.builder()
                        .location(point)
                        .offset(0, 0, 0)
                        .count(1)
                        .receivers(receivers)
                        .extra(0)
                        .spawn();
            }
        }

        @Override
        protected boolean canCollideWith(Entity entity) {
            if (!super.canCollideWith(entity) || hitTargets.contains(entity)) {
                return false;
            }

            final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(caster, (LivingEntity) entity);
            event.callEvent();
            return event.getResult() != Event.Result.DENY;
        }

        @Override
        protected void onImpact(Location location, RayTraceResult result) {
            if (result.getHitBlock() != null) {
                markForRemoval = true;
                return;
            }

            final LivingEntity target = (LivingEntity) Objects.requireNonNull(result.getHitEntity());
            hitTargets.add(target);

            CustomDamageEvent event = new CustomDamageEvent(target,
                    caster,
                    null,
                    EntityDamageEvent.DamageCause.CUSTOM,
                    slashDamage,
                    false,
                    "Wind Burst");
            UtilDamage.doCustomDamage(event);

            if (event.isCancelled() || caster == null || !caster.isOnline()) {
                return; // Cancelled or offline, dont do anything
            }

            // Knockback
            final Vector direction = this.velocity.clone().normalize();
            UtilVelocity.velocity(target, caster, new VelocityData(
                    direction.clone(),
                    slashVelocity,
                    0,
                    slashVelocity,
                    true
            ));

            // SFX
            new SoundEffect(Sound.ENTITY_PUFFER_FISH_STING, 0.8F, 1.5F).play(target.getLocation());

            // Regen energy
            energyService.regenerateEnergy(caster, slashEnergyRefundPercent, EnergyEvent.CAUSE.CUSTOM);
        }
    }
}