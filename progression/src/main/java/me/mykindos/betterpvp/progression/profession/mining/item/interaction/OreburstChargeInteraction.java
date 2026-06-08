package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import me.mykindos.betterpvp.core.locale.Translations;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableHandler;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.progression.profession.mining.util.MiningDetonation;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Throwable detonation that carves a sphere and lines its shell with ore — like
 * {@link ExplosiveExcavationInteraction#detonate} — but with a fallback for protected
 * areas: when a per-block break is denied (Fields/clans), the block is instead
 * converted to an ore in place if it qualifies as an exposed surface block, so the
 * charge still produces ore deposits without altering terrain.
 */
@Getter
public class OreburstChargeInteraction extends CooldownInteraction implements ThrowableListener, DisplayedInteraction {

    @Setter private double cooldown;
    @Setter private double throwableExpiry;
    @Setter private double throwSpeed;
    @Setter private int radius;
    @Setter private double oreChance;
    @Setter private Supplier<Material> oreSupplier = () -> null;

    private final ThrowableHandler throwableHandler;
    private final BlockTagManager blockTagManager;

    public OreburstChargeInteraction(CooldownManager cooldownManager,
                                     ThrowableHandler throwableHandler,
                                     BlockTagManager blockTagManager,
                                     double cooldown,
                                     double throwableExpiry,
                                     double throwSpeed,
                                     int radius,
                                     double oreChance) {
        super("oreburst_charge", cooldownManager);
        this.throwableHandler = throwableHandler;
        this.blockTagManager = blockTagManager;
        this.cooldown = cooldown;
        this.throwableExpiry = throwableExpiry;
        this.throwSpeed = throwSpeed;
        this.radius = radius;
        this.oreChance = oreChance;
    }

    @Override
    public double getCooldown(InteractionActor actor) {
        return cooldown;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("progression.ability.oreburst-charge.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("progression.ability.oreburst-charge.description");
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor,
                                                            @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance,
                                                            @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.FIREWORK_STAR));
        item.setVelocity(player.getLocation().getDirection().multiply(throwSpeed));
        item.setGlowing(true);

        ThrowableItem throwable = new ThrowableItem(this, item, player, "Oreburst Charge",
                (long) (throwableExpiry * 1000L), true);
        throwable.setCollideGround(true);
        throwable.getImmunes().add(player);
        throwableHandler.addThrowable(throwable);

        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 1.4f).play(player);
        return InteractionResult.Success.ADVANCE;
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        handleDetonation(throwableItem);
    }

    @Override
    public void onThrowableHitGround(ThrowableItem throwableItem, LivingEntity thrower, Location location) {
        handleDetonation(throwableItem);
    }

    private void handleDetonation(ThrowableItem throwableItem) {
        if (!(throwableItem.getThrower() instanceof Player player)) {
            throwableItem.getItem().remove();
            return;
        }
        detonate(player, throwableItem.getItem().getLocation(), radius, oreChance, oreSupplier);
        throwableItem.getItem().remove();
    }

    /**
     * Sphere detonation. Mirrors {@link ExplosiveExcavationInteraction#detonate} but adds a
     * surface-fallback: when {@code breakBlock} was denied, an exposed face still gets
     * converted to ore in place so the charge produces deposits in protected zones.
     */
    public void detonate(Player player, Location center, int radius, double oreChance,
                         Supplier<Material> oreSupplier) {
        MiningDetonation.detonate(player, center, radius, oreChance, oreSupplier,
                "progression:oreburst_charge", blockTagManager, false,
                ctx -> MiningDetonation.isExposedSurfaceBlock(ctx.block()));

        Particle.FLASH.builder()
                .count(1)
                .color(Color.ORANGE)
                .location(center.toCenterLocation())
                .receivers(60)
                .spawn();
        Particle.EXPLOSION.builder()
                .count(1)
                .location(center.toCenterLocation())
                .receivers(60)
                .spawn();
        new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f).play(center);
        new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, (float) Math.random(), 0.5f).play(center);
    }
}
