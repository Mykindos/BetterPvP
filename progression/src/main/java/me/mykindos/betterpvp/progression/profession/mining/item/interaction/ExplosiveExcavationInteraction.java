package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.blockbreak.event.ScriptedBlockPlaceEvent;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ExplosiveExcavationInteraction extends AbstractInteraction implements DisplayedInteraction {

    private static final Random RANDOM = new Random();
    private static final Set<UUID> DETONATING = ConcurrentHashMap.newKeySet();

    /**
     * Locations of blocks currently being broken inside {@link #detonate}. Read by
     * {@code ExplosiveExcavationSilencer} (a {@code @PluginAdapter("Clans")} listener) which
     * flips {@code TerritoryInteractEvent#setInform(false)} for any matching block, suppressing
     * the "You cannot break X in Clan Y" spam that would otherwise fire once per skipped block.
     * The TTL is a safety net — entries are normally invalidated synchronously after each break.
     */
    private static final Cache<Location, Boolean> SILENT_BREAKS = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public static boolean isSilentBreak(Location location) {
        return SILENT_BREAKS.getIfPresent(location) != null;
    }

    @Setter private double triggerChance;
    @Setter private int radius;
    @Setter private double oreChance;
    @Setter private Supplier<Material> oreSupplier = () -> null;

    @SuppressWarnings("unused")
    private final ItemFactory itemFactory;
    private final BlockTagManager blockTagManager;

    public ExplosiveExcavationInteraction(ItemFactory itemFactory, BlockTagManager blockTagManager,
                                          double triggerChance, int radius, double oreChance) {
        super("Explosive Excavation");
        this.itemFactory = itemFactory;
        this.triggerChance = triggerChance;
        this.radius = radius;
        this.oreChance = oreChance;
        this.blockTagManager = blockTagManager;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor,
                                                    @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance,
                                                    @Nullable ItemStack itemStack) {
        if (!actor.isPlayer()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        Player player = (Player) actor.getEntity();

        Block broken = context.get(InputMeta.BROKEN_BLOCK).orElse(null);
        if (broken == null || !UtilBlock.isStoneBased(broken)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (RANDOM.nextDouble() >= triggerChance) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        detonate(player, broken.getLocation(), radius, oreChance, oreSupplier);
        return InteractionResult.Success.ADVANCE;
    }

    /**
     * Carves an imperfect sphere centered on {@code center}. Each candidate block within the sphere
     * is broken via {@link UtilBlock#breakBlock} — which serves both as the destruction call and as
     * the permission probe (returns false when clans/fields/world rules deny the break, in which
     * case the block stays and the position is skipped). Blocks on the outer shell of the sphere
     * are replaced with an ore block (chosen by {@code oreSupplier}) after a successful break,
     * lining the crater wall with mineable ore rather than dropping ore items.
     *
     * @param player       the player responsible for the explosion
     * @param center       the center of the sphere
     * @param radius       the integer radius of the sphere; jittered ±0.8 per block for an organic shape
     * @param oreChance    probability (0.0–1.0) for each shell block to be replaced with an ore variant
     * @param oreSupplier  function returning the ore Material for a single shell-replacement;
     *                     return {@code null} to skip a given replacement
     */
    public void detonate(Player player, Location center, int radius, double oreChance,
                                 Supplier<Material> oreSupplier) {
        // Recursion guard: Player#breakBlock fires BlockBreakEvent → InteractionListener →
        // potentially re-routes to this interaction. Without the guard, one mine would chain.
        final UUID id = player.getUniqueId();
        if (!DETONATING.add(id)) return;
        try {
            final World world = center.getWorld();
            final double rSq = radius * radius;
            // Anything in the outer ~30% by squared distance is treated as crater shell.
            final double shellThresholdSq = rSq * 0.7;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        final double distSq = dx * dx + dy * dy + dz * dz;

                        // Per-block jitter on the cutoff produces an organic, imperfect sphere
                        // rather than a clean geometric ball.
                        final double jitter = (RANDOM.nextDouble() - 0.5) * 1.6;
                        if (distSq > rSq + jitter) continue;

                        final Block block = world.getBlockAt(
                                center.getBlockX() + dx,
                                center.getBlockY() + dy,
                                center.getBlockZ() + dz);

                        if (block.equals(center.getBlock())) continue; // Don't break the block that was just mined to trigger this
                        if (!UtilBlock.isStoneBased(block)) continue;
                        if (blockTagManager.isPlayerPlaced(block)) continue; // Skip player-placed blocks to avoid people making grinding setups

                        // Mark this location so the clans-side silencer can suppress
                        // TerritoryInteractEvent.inform for the BlockBreakEvent that
                        // Player#breakBlock is about to fire. Cleared right after.
                        final Location key = block.getLocation();
                        SILENT_BREAKS.put(key, Boolean.TRUE);
                        try {
                            UtilBlock.breakBlock(player, block);
                        } finally {
                            SILENT_BREAKS.invalidate(key);
                        }

                        if (distSq >= shellThresholdSq && RANDOM.nextDouble() < oreChance) {
                            final Material ore = oreSupplier.get();
                            if (ore != null) {
                                final BlockData oreData = ore.createBlockData();
                                final ScriptedBlockPlaceEvent event = new ScriptedBlockPlaceEvent(
                                        player,
                                        block,
                                        block.getBlockData(),
                                        oreData,
                                        "progression:explosive_excavation"
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
            }

            Particle.FLASH.builder()
                    .count(1)
                    .color(Color.AQUA)
                    .location(center.toCenterLocation())
                    .receivers(60)
                    .spawn();
            new SoundEffect(Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, (float) (1 + Math.random()), 0.5f).play(center);
            new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, (float) (0 + Math.random()), 0.5f).play(center);
        } finally {
            DETONATING.remove(id);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Explosive Excavation");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Mining has a chance to carve an underground crater whose walls are lined with ore.");
    }
}
