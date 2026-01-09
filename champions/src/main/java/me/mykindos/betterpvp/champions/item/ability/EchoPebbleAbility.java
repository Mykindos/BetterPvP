package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@CustomLog
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EchoPebbleAbility extends ItemAbility implements ThrowableListener {

    @EqualsAndHashCode.Include
    private double velocity;

    @EqualsAndHashCode.Include
    private double revealDuration;

    @EqualsAndHashCode.Include
    private double radius;

    @EqualsAndHashCode.Include
    private boolean needsLineOfSight;

    @EqualsAndHashCode.Include
    private double cooldown;

    @EqualsAndHashCode.Include
    private double throwableExpiry;

    private final Champions champions;
    private final ChampionsManager championsManager;
    private final CooldownManager cooldownManager;
    
    private final Set<@NotNull LivingEntity> entitiesAlreadyHit = Collections.newSetFromMap(new WeakHashMap<>());

    /*
    TODO:
    - test item model
     */

    public EchoPebbleAbility(Champions champions, ChampionsManager championsManager, CooldownManager cooldownManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "echo_pebble"),
                "Echo Pebble",
                "Toss a pebble that reveals the location of hidden enemies in a radius upon landing. Vanished enemies are not revealed.",
                TriggerTypes.LEFT_CLICK);

        this.champions = champions;
        this.championsManager = championsManager;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        final @NotNull Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        if (!cooldownManager.use(player, getName(), (float) getCooldown(), true, true)) {
            return false;
        }

        final @NotNull Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.BAKED_POTATO));
        item.getItemStack().editMeta(meta -> {
            meta.getPersistentDataContainer().set(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        });

        item.setVelocity(player.getLocation().getDirection().multiply(velocity));
        final @NotNull ThrowableItem throwableItem = new ThrowableItem(this, item, player, getName(),
                (long) (throwableExpiry * 1000L), true);

        throwableItem.setCollideGround(true);
        throwableItem.getImmunes().add(player);
        championsManager.getThrowables().addThrowable(throwableItem);
        return true;
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        handleCollision(thrower, throwableItem);
    }

    @Override
    public void onThrowableHitGround(ThrowableItem throwableItem, LivingEntity thrower, org.bukkit.Location location) {
        handleCollision(thrower, throwableItem);
    }

    private void handleCollision(@NotNull LivingEntity thrower, @NotNull ThrowableItem throwableItem) {
        if (!(thrower instanceof Player player)) {
            throwableItem.getItem().remove();
            log.error("EchoPebbleAbility is only meant to be used by players.");
            return;
        }

        final @NotNull Location impactLocation = throwableItem.getItem().getLocation();

        doExplosion(player, impactLocation, radius * (1d/3), 5, 0L, 0.5f);
        doExplosion(player, impactLocation, radius * (2d/3), 15, 10L, 1f);
        doExplosion(player, impactLocation, radius, 25, 15L, 2f);

        throwableItem.getItem().remove();
    }

    private void doExplosion(@NotNull Player player, @NotNull Location impactLocation, double radius, int points,
                             long explosionDelay, float size) {
        final @NotNull List<Location> spherePoints = UtilLocation.getSphere(impactLocation, radius, points);

        UtilServer.runTaskLater(champions, () -> {
            for (@NotNull LivingEntity livingEntity : UtilEntity.getNearbyEnemies(player, impactLocation, radius)) {
                if (needsLineOfSight && !livingEntity.hasLineOfSight(impactLocation)) continue;
                if (entitiesAlreadyHit.contains(livingEntity)) continue;

                UtilPlayer.setGlowingForPlayerAndAllies(player, livingEntity, true, 60);
                entitiesAlreadyHit.add(livingEntity);

                final long revealDelay = (long) (revealDuration * 20L);

                UtilServer.runTaskLater(champions, () -> {
                    UtilPlayer.setGlowingForPlayerAndAllies(player, livingEntity, false, 60);
                    entitiesAlreadyHit.remove(livingEntity);
                }, revealDelay);
            }
        }, explosionDelay);

        spherePoints.forEach(point -> UtilServer.runTaskLater(champions, () -> playVfxAndSfx(impactLocation, point, size), explosionDelay));
    }

    private void playVfxAndSfx(@NotNull Location impactLocation, @NotNull Location point, float size) {

        final double randomNum = UtilMath.randDouble(0.0, 1.0);
        final @NotNull Color color;

        if (randomNum < 0.33) color = Color.PURPLE;
        else if (randomNum < 0.66) color = Color.TEAL;
        else color = Color.AQUA;

        new ParticleBuilder(Particle.DUST)
                .location(point)
                .count(1)
                .extra(1)
                .data(new Particle.DustOptions(color, size))
                .receivers(60)
                .spawn();

        impactLocation.getWorld().playSound(impactLocation, Sound.BLOCK_CHORUS_FLOWER_GROW, 1f, 2f);
    }

}
