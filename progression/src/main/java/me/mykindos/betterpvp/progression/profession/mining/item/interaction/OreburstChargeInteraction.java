package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableHandler;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.blockbreak.event.ScriptedBlockPlaceEvent;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Throwable detonation that carves a sphere and lines its shell with ore — like
 * {@link ExplosiveExcavationInteraction#detonate} — but with a fallback for protected
 * areas: when a per-block break is denied (Fields/clans), the block is instead
 * converted to an ore in place if it's an exposed stone-based surface block, so the
 * charge still produces ore deposits without altering terrain.
 */
@Getter
public class OreburstChargeInteraction extends CooldownInteraction implements ThrowableListener, DisplayedInteraction {

    private static final Random RANDOM = new Random();
    private static final Set<UUID> DETONATING = ConcurrentHashMap.newKeySet();

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
        return Component.text("Oreburst Charge");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Throw a charge that detonates on impact, carving a crater lined with ores.");
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
     * Sphere detonation. Mirrors {@link ExplosiveExcavationInteraction#detonate} but also runs
     * a Fields-fallback: when {@link UtilBlock#breakBlock} is denied, the block is converted to
     * ore in place if it qualifies as an exposed surface block.
     */
    public void detonate(Player player, Location center, int radius, double oreChance,
                         Supplier<Material> oreSupplier) {
        final UUID id = player.getUniqueId();
        if (!DETONATING.add(id)) return;
        try {
            final World world = center.getWorld();
            final double rSq = radius * radius;
            final double shellThresholdSq = rSq * 0.7;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        final double distSq = dx * dx + dy * dy + dz * dz;
                        final double jitter = (RANDOM.nextDouble() - 0.5) * 1.6;
                        if (distSq > rSq + jitter) continue;

                        final Block block = world.getBlockAt(
                                center.getBlockX() + dx,
                                center.getBlockY() + dy,
                                center.getBlockZ() + dz);

                        if (!UtilBlock.isStoneBased(block)) continue;
                        if (blockTagManager.isPlayerPlaced(block)) continue;

                        final Location key = block.getLocation();
                        final BlockData previousData = block.getBlockData();
                        ExplosiveExcavationInteraction.markSilent(key);
                        boolean broken;
                        try {
                            broken = UtilBlock.breakBlock(player, block);
                        } finally {
                            ExplosiveExcavationInteraction.unmarkSilent(key);
                        }

                        boolean isShell = broken && distSq < shellThresholdSq;
                        boolean isSurface = isExposedSurfaceBlock(block);
                        if ((isShell || isSurface) && RANDOM.nextDouble() < oreChance) {
                            final Material ore = oreSupplier.get();
                            if (ore == null) return;
                            final BlockData oreData = ore.createBlockData();
                            final ScriptedBlockPlaceEvent event = new ScriptedBlockPlaceEvent(
                                    player,
                                    block,
                                    previousData,
                                    oreData,
                                    "progression:oreburst_charge:fields_fallback"
                            );
                            event.callEvent();
                            if (!event.isCancelled()) {
                                block.setBlockData(oreData);
                                UtilBlock.playBlockEffect(block, oreData);
                            }
                        }
                    }
                }
            }

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
            new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, (float) (Math.random()), 0.5f).play(center);
        } finally {
            DETONATING.remove(id);
        }
    }

    /**
     * Decides whether {@code block} qualifies as an "exposed surface" block — the fallback
     * condition that lets the charge still place ore deposits in protected areas.
     *
     * <p><b>TODO (user contribution):</b> implement this. The call site has already filtered
     * to stone-based, non-player-placed blocks inside the explosion radius. You only need to
     * decide what counts as "surface" — i.e. visible enough to the player that converting it
     * feels natural rather than spooky-action-at-a-distance.
     *
     * <p>Trade-offs to consider:
     * <ul>
     *   <li>Only check the block above (BlockFace.UP) — strict "topsoil" feel; few conversions
     *       on cliff faces or cave walls.</li>
     *   <li>Check all 6 faces for any passable / air neighbor — generous; lights up cave
     *       ceilings and walls too. Often the "feels good" choice for this kind of grenade.</li>
     *   <li>Check only the 4 horizontal faces + UP — middle ground; ignores floors above caves.</li>
     * </ul>
     * Use {@link Block#getRelative(BlockFace)} and {@link Block#isPassable()} /
     * {@link Material#isAir()} to probe neighbors.
     */
    private boolean isExposedSurfaceBlock(Block block) {
        final List<BlockFace> faces = List.of(BlockFace.UP,
                BlockFace.DOWN,
                BlockFace.NORTH,
                BlockFace.SOUTH,
                BlockFace.EAST,
                BlockFace.WEST);
        for (BlockFace face : faces) {
            if (block.getRelative(face).getType().isAir()) return true;
        }
        return false;
    }
}
