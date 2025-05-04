package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class AlligatorsTooth extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String SWIM_ABILITY = "Swim";
    private static final String HURL_ABILITY = "Gator Strike";

    @Getter
    private double velocityStrength;
    @Getter
    private double swimmingEnergyMultiplier;
    @Getter
    private double strikeEffectDuration;
    @Getter
    private int strikePinAmplifier;
    @Getter
    private double strikeVelocityStrength;
    @Getter
    private double strikeEnergy;
    @Getter
    private double strikeAirDuration;

    private final Map<Player, StrikeData> activeHurls = new WeakHashMap<>();
    private final EnergyHandler energyHandler;
    private final EffectManager effectManager;
    private final ClientManager clientManager;
    private final CooldownManager cooldownManager;

    @Inject
    public AlligatorsTooth(Champions champions, EnergyHandler energyHandler, EffectManager effectManager, ClientManager clientManager, CooldownManager cooldownManager) {
        super(champions, "alligators_tooth");
        this.energyHandler = energyHandler;
        this.effectManager = effectManager;
        this.clientManager = clientManager;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("This deadly tooth was stolen from", NamedTextColor.WHITE));
        lore.add(Component.text("a nest of reptilian beasts long ago.", NamedTextColor.WHITE));
        lore.add(Component.text("Legends say that the holder is granted", NamedTextColor.WHITE));
        lore.add(Component.text("the underwater agility of an Alligator.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f</yellow> Damage", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click<white> to use <green>" + HURL_ABILITY));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click in Water<white> to use <green>%s", SWIM_ABILITY));
        return lore;
    }

    @Override
    public void activate(Player player) {
        if (UtilBlock.isInWater(player)) {
            active.add(player.getUniqueId());
        } else {
            final Vector vector = player.getLocation().getDirection();
            final VelocityData data = new VelocityData(vector, getStrikeVelocityStrength(), 0, 10.0, true);
            UtilVelocity.velocity(player, player, data);
            new SoundEffect(Sound.ENTITY_CAT_HISS, 0.6f, 1.5f).play(player.getLocation());
            new SoundEffect(Sound.ENTITY_WOLF_PANT, 1.5f).play(player.getLocation());
            activeHurls.put(player, new StrikeData(UtilPlayer.getMidpoint(player), false));
            UtilMessage.simpleMessage(player, "Gator Strike", "You used <alt>%s</alt>.", HURL_ABILITY);
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInWater(player)) {
            return true;
        }

        return this.cooldownManager.use(player, HURL_ABILITY, 0.1, false)
                && this.energyHandler.use(player, HURL_ABILITY, getStrikeEnergy(), true);
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

    @UpdateEvent
    public void onHurl() {
        if (!enabled) {
            return;
        }

        final Iterator<Map.Entry<Player, StrikeData>> iterator = activeHurls.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, StrikeData> entry = iterator.next();
            final Player player = entry.getKey();
            final StrikeData data = entry.getValue();
            final Location lastLocation = data.getLastLocation();
            if (player == null || !player.isValid() || UtilTime.elapsed(data.getStart(), (long) (getStrikeAirDuration() * 1000))) {
                iterator.remove();
                continue;
            }

            if (UtilBlock.isGrounded(player)) {
                data.setGrounded(true);
            } else {
                Particle.BUBBLE_POP.builder()
                        .count(20)
                        .extra(0)
                        .offset(0.5, 0.5, 0.5)
                        .location(lastLocation)
                        .receivers(60)
                        .spawn();
                new SoundEffect(Sound.ENTITY_PLAYER_SPLASH, 0.5f, 1f).play(player.getLocation());
                new ParticleBuilder(Particle.BLOCK)
                        .data(Material.LAPIS_BLOCK.createBlockData())
                        .count(50)
                        .location(player.getLocation())
                        .offset(0.7, 0.7, 0.7)
                        .extra(0)
                        .receivers(60)
                        .spawn();
                player.setFallDistance(0);
                player.startRiptideAttack(1, 0, new ItemStack(Material.TRIDENT));
            }

            // Get the enemy that collides with the player from the last location to the new location
            final Location newLocation = UtilPlayer.getMidpoint(player);
            data.setLastLocation(newLocation);
            final Optional<LivingEntity> hitEntityOpt = UtilEntity.interpolateCollision(lastLocation,
                            newLocation,
                            1.4f,
                            ent -> {
                                if (ent instanceof LivingEntity livingEntity) {
                                    EntityCanHurtEntityEvent entityCanHurtEntityEvent = UtilServer.callEvent(new EntityCanHurtEntityEvent(player, livingEntity));
                                    return entityCanHurtEntityEvent.isAllowed();
                                }

                                return ent instanceof LivingEntity;
                            })
                    .stream()
                    .map(RayTraceResult::getHitEntity)
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast)
                    .findFirst();

            // Cues
            if (hitEntityOpt.isEmpty()) {
                continue;
            }

            // remove because we hit
            iterator.remove();
            player.setVelocity(player.getVelocity().multiply(0.6));

            // Effects
            final LivingEntity hitEntity = hitEntityOpt.get();
            this.effectManager.addEffect(hitEntity, player, EffectTypes.PIN, HURL_ABILITY, getStrikePinAmplifier(), (long) (getStrikeEffectDuration() * 1000));
            this.effectManager.addEffect(hitEntity, player, EffectTypes.BLEED, HURL_ABILITY, 1, (long) (getStrikeEffectDuration() * 1000));
            UtilMessage.simpleMessage(player, "Gator Strike", "You hit <alt2>%s</alt2> with <alt>%s</alt>", hitEntity.getName(), HURL_ABILITY);
            UtilMessage.simpleMessage(hitEntity, "Gator Strike", "<alt2>%s</alt2> hit you with <alt>%s</alt>.", player.getName(), HURL_ABILITY);

            // Cues
            new SoundEffect(Sound.ENTITY_WOLF_GROWL, 0.6f, 1.5f).play(player.getLocation());
            new SoundEffect(Sound.ENTITY_HOSTILE_SPLASH, 0f, 0.6f).play(player.getLocation());
            new ParticleBuilder(Particle.CRIT)
                    .location(player.getLocation())
                    .offset(0.7, 0.7, 0.7)
                    .count(5)
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }
    }

    @UpdateEvent
    public void doAlligatorsTooth() {
        if (!enabled) {
            return;
        }

        active.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return true;

            if (!isHoldingWeapon(player)) {
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            final Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            if (!canUse(player)) {
                return false;
            }

            var energyToUse = energyPerTick;
            if (!UtilBlock.isWater(player.getEyeLocation().getBlock().getRelative(BlockFace.DOWN))) {
                energyToUse *= swimmingEnergyMultiplier;
            }

            if (!energyHandler.use(player, SWIM_ABILITY, energyToUse, true)) {
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), velocityStrength, false, 0, 0.11, 1.0, true);
            UtilVelocity.velocity(player, null, velocityData);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8F, 1.5F);
            this.cooldownManager.use(player, HURL_ABILITY, 0.5, false);
            return false;
        });
    }

    @UpdateEvent(delay = 1000)
    public void onOxygenDrain() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!UtilBlock.isInWater(player)) continue;
            if (!isHoldingWeapon(player)) continue;
            player.setRemainingAir(player.getMaximumAir());
        }
    }

    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

    @Override
    public void loadWeaponConfig() {
        velocityStrength = getConfig("velocityStrength", 0.7, Double.class);
        swimmingEnergyMultiplier = getConfig("swimmingEnergyMultiplier", 3.0, Double.class);
        strikeEffectDuration = getConfig("strikeEffectDuration", 5.0, Double.class);
        strikePinAmplifier = getConfig("strikePinAmplifier", 10, Integer.class);
        strikeVelocityStrength = getConfig("strikeVelocityStrength", 0.7, Double.class);
        strikeEnergy = getConfig("strikeEnergy", 20.0, Double.class);
        strikeAirDuration = getConfig("strikeAirDuration", 2.0, Double.class);
    }

    @Data
    @AllArgsConstructor
    private static class StrikeData {
        private final long start = System.currentTimeMillis();
        private Location lastLocation;
        private boolean grounded;
    }
}