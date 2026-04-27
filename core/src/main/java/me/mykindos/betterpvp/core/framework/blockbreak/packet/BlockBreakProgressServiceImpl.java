package me.mykindos.betterpvp.core.framework.blockbreak.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.event.block.BlockBreakProgressUpdateEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.blockbreak.ToolMiningSpeed;
import me.mykindos.betterpvp.core.framework.blockbreak.resolver.BlockBreakResolver;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Drives all block-break sessions. Lifecycle:
 * <ol>
 *   <li>Player presses left click → client sends {@code START_DIGGING}.</li>
 *   <li>Packet listener (netty thread) cancels the packet so vanilla never runs,
 *       inserts a session into the concurrent map <i>immediately</i>, and
 *       schedules {@link BlockDamageEvent} dispatch on the main thread.</li>
 *   <li>{@link #tick()} runs every server tick on the main thread, advances
 *       progress on each session, sends animation deltas, and breaks blocks
 *       that reach 100% via {@link Player#breakBlock(Block)} — which fires
 *       {@code BlockBreakEvent} natively, applies tool damage, plays break
 *       sounds, and drops items respecting tool tier.</li>
 *   <li>{@code CANCELLED_DIGGING} (release / target change) clears the session
 *       and the overlay <i>immediately on the netty thread</i> — no tick wait —
 *       so a quick tap doesn't leave a lingering destruction stage.</li>
 * </ol>
 *
 * Threading: session map is a {@link ConcurrentHashMap}, so listener-thread
 * inserts and main-thread reads are safe. Animation packet sends are
 * thread-safe per PacketEvents. Bukkit event firing and {@link Block} mutation
 * happen exclusively on the main thread.
 */
@SuppressWarnings("ALL")
@CustomLog
@BPvPListener
@Singleton
public class BlockBreakProgressServiceImpl implements BlockBreakProgressService, Listener {

    private static final int VIEW_RADIUS_SQ = 64 * 64;
    /** Synthetic entity ids for animation packets — negative to avoid colliding with real entities. */
    private static final AtomicInteger ENTITY_ID_SOURCE = new AtomicInteger(-2_000_000_000);

    /**
     * Vanilla inter-break delay: 6 ticks (300 ms) between consecutive non-instant breaks.
     * Instant breaks (damage ≥ 1 in a single tick) bypass this entirely — see Minecraft Wiki "Breaking".
     */
    private static final long INTER_BREAK_COOLDOWN_MS = 300L;

    private final BlockBreakResolver resolver;
    /** One active session per player; new dig replaces previous. */
    private final ConcurrentHashMap<UUID, BreakSession> sessions = new ConcurrentHashMap<>();
    /** Wall-clock millis at which the player's next non-instant break may begin accumulating progress. */
    private final ConcurrentHashMap<UUID, Long> nextAllowedProgressMillis = new ConcurrentHashMap<>();

    @Inject
    public BlockBreakProgressServiceImpl(BlockBreakResolver resolver) {
        this.resolver = resolver;
        PacketEvents.getAPI().getEventManager().registerListener(new DigListener(), PacketListenerPriority.HIGH);
    }

    // ─── Public API ──────────────────────────────────────────────────────────────

    @Override
    public void startSession(@NotNull Player player, @NotNull UUID worldUid, int x, int y, int z) {
        cancelSessionFor(player.getUniqueId()); // replace any previous

        final BreakSession session = new BreakSession(
                player.getUniqueId(), worldUid,
                new Vector3i(x, y, z),
                ENTITY_ID_SOURCE.getAndIncrement());
        sessions.put(player.getUniqueId(), session);
    }

    @Override
    public void cancelSessionFor(@NotNull UUID playerId) {
        final BreakSession session = sessions.remove(playerId);
        if (session == null) return;
        clearOverlay(session); // immediate, thread-safe
        // BlockDamageAbortEvent must fire on the main thread; defer if we're not on it.
        if (Bukkit.isPrimaryThread()) {
            fireDamageAbort(session);
        } else {
            Bukkit.getScheduler().runTask(
                    JavaPlugin.getProvidingPlugin(BlockBreakProgressServiceImpl.class),
                    () -> fireDamageAbort(session));
        }
    }

    private void fireDamageAbort(BreakSession session) {
        final Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null) return;
        final World world = Bukkit.getWorld(session.getWorldUid());
        if (world == null) return;
        final Vector3i pos = session.getBlockPos();
        final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
        Bukkit.getPluginManager().callEvent(
                new BlockDamageAbortEvent(player, block, player.getInventory().getItemInMainHand()));
    }

    @Override
    public void cancelSessionsAt(@NotNull UUID worldUid, int x, int y, int z) {
        final Iterator<Map.Entry<UUID, BreakSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            final BreakSession s = it.next().getValue();
            if (s.getWorldUid().equals(worldUid)
                    && s.getBlockPos().getX() == x
                    && s.getBlockPos().getY() == y
                    && s.getBlockPos().getZ() == z) {
                clearOverlay(s);
                it.remove();
            }
        }
    }

    // ─── Tick loop (main thread) ─────────────────────────────────────────────────

    @UpdateEvent(delay = 50) // 1 server tick
    public void tick() {
        if (sessions.isEmpty()) return;

        final List<BreakSession> snapshot = new ArrayList<>(sessions.values());
        for (BreakSession session : snapshot) {
            tickOne(session);
        }
    }

    private void tickOne(BreakSession session) {
        final Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null || !player.isOnline()) {
            sessions.remove(session.getPlayerId());
            nextAllowedProgressMillis.remove(session.getPlayerId());
            return;
        }

        final World world = Bukkit.getWorld(session.getWorldUid());
        if (world == null) {
            cancelSessionFor(session.getPlayerId());
            return;
        }

        final Vector3i pos = session.getBlockPos();
        final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());

        final ItemStack held = player.getInventory().getItemInMainHand();
        final BlockBreakProperties props = resolver.resolve(player, block, held);
        if (!props.isBreakable()) {
            cancelSessionFor(session.getPlayerId());
            return;
        }

        final float hardness = block.getType().getHardness();
        if (hardness < 0f) {
            cancelSessionFor(session.getPlayerId());
            return;
        }

        // Unconditional instant breaks (Wiki "Breaking → Instant breaking"):
        //   - Hardness 0 blocks (grass, flowers, fire, etc.) — speed/0 = ∞ trumps any penalty.
        //   - Creative mode, except with non-mining items the wiki lists explicitly.
        // Bypass progress accumulation entirely so we don't leave a session that drips progress
        // when the client doesn't continuously send digging packets (creative single-click).
        if (hardness == 0f || isCreativeInstant(player, held)) {
            completeBreak(player, block, session, true);
            return;
        }

        final double basePerTick =
                (double) props.getBreakSpeed() / (ToolMiningSpeed.VANILLA_TICK_DIVISOR * hardness);

        // Player-state modifiers are multiplicative on damage per tick (Wiki "Breaking → Calculation").
        final double progressPerTick = basePerTick * playerStateMultiplier(player);

        // Wiki "Breaking": damage ≥ 1 in a single tick = instant break. progressPerTick is exactly that
        // damage value in our scaled units, so this check is equivalent to speedMultiplier > 30 * hardness.
        final boolean instant = progressPerTick >= 1.0;

        // Non-instant breaks must respect the 6-tick post-completion delay. Instant breaks chain freely.
        if (!instant) {
            final Long gateUntil = nextAllowedProgressMillis.get(session.getPlayerId());
            if (gateUntil != null && System.currentTimeMillis() < gateUntil) {
                return;
            }
        }

        session.setProgress(session.getProgress() + progressPerTick);

        if (session.getProgress() >= 1.0) {
            completeBreak(player, block, session, instant);
            return;
        }

        final int stage = Math.min(9, Math.max(0, (int) Math.floor(session.getProgress() * 10)));
        if (stage != session.getLastStageSent()) {
            session.setLastStageSent(stage);
            broadcastAnimation(world, pos, session.getAnimationEntityId(), (byte) stage);
            // Notify Paper-aware plugins that progress advanced. Already on main thread.
            Bukkit.getPluginManager().callEvent(new BlockBreakProgressUpdateEvent(
                    block, (float) Math.min(1.0, session.getProgress()), player));
        }
    }

    private void completeBreak(Player player, Block block, BreakSession session, boolean instant) {
        sessions.remove(session.getPlayerId());
        clearOverlay(session);

        if (instant) {
            nextAllowedProgressMillis.remove(session.getPlayerId());
        } else {
            nextAllowedProgressMillis.put(
                    session.getPlayerId(),
                    System.currentTimeMillis() + INTER_BREAK_COOLDOWN_MS);
        }

        final ItemStack tool = player.getInventory().getItemInMainHand();
        if (breakBlock(player, block)) {
            cancelSessionsAt(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    private boolean breakBlock(Player player, Block block) {
        final BlockState state = ((CraftBlock) block).getNMS();
        final boolean success = player.breakBlock(block);
        if (success) {
            // copied from NMS
            final LevelAccessor world = ((CraftBlock) block).getHandle();
            final BlockPos position = ((CraftBlock) block).getPosition();
            if (state.getBlock() instanceof BaseFireBlock) {
                world.levelEvent(1009, position, 0);
            } else {
                world.levelEvent(2001, position, net.minecraft.world.level.block.Block.getId(state));
            }
        }
        return success;
    }

    /**
     * Combined multiplier for the four vanilla player-state modifiers (Minecraft Wiki "Breaking").
     * <ul>
     *   <li>Haste: ×(1 + 0.2 × level)</li>
     *   <li>Mining Fatigue: ×0.3<sup>min(level, 4)</sup></li>
     *   <li>Head submerged in water without Aqua Affinity: ×0.2</li>
     *   <li>Feet not on ground: ×0.2</li>
     * </ul>
     * Bukkit potion amplifiers are 0-indexed (Haste I → amplifier 0), so we add 1 to get level.
     */
    private double playerStateMultiplier(Player player) {
        double multiplier = 1.0;

        final PotionEffect haste = player.getPotionEffect(PotionEffectType.HASTE);
        if (haste != null) {
            multiplier *= 1.0 + 0.2 * (haste.getAmplifier() + 1);
        }

        final PotionEffect fatigue = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (fatigue != null) {
            final int level = Math.min(fatigue.getAmplifier() + 1, 4);
            multiplier *= Math.pow(0.3, level);
        }

        if (isHeadInWater(player) && !hasAquaAffinity(player)) {
            multiplier *= 0.2;
        }

        if (!player.isOnGround()) {
            multiplier *= 0.2;
        }

        return multiplier;
    }

    private static boolean isHeadInWater(Player player) {
        final Location eyes = player.getEyeLocation();
        final Material eyeBlock = eyes.getBlock().getType();
        return eyeBlock == Material.WATER || eyeBlock == Material.BUBBLE_COLUMN;
    }

    /**
     * Wiki "Breaking → Instant breaking": creative mode breaks any block instantly except when
     * holding swords, tridents, maces, or debug sticks (Java Edition). Spears don't exist in
     * vanilla Java.
     */
    private static boolean isCreativeInstant(Player player, ItemStack held) {
        if (player.getGameMode() != GameMode.CREATIVE) return false;
        if (held == null) return true;
        final Material type = held.getType();
        if (Tag.ITEMS_SWORDS.isTagged(type)) return false;
        return type != Material.TRIDENT && type != Material.MACE && type != Material.DEBUG_STICK;
    }

    private static boolean hasAquaAffinity(Player player) {
        final ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null && helmet.containsEnchantment(Enchantment.AQUA_AFFINITY);
    }

    // ─── Bukkit event hooks ──────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final UUID playerId = event.getPlayer().getUniqueId();
        cancelSessionFor(playerId);
        nextAllowedProgressMillis.remove(playerId);
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        cancelSessionFor(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final AttributeInstance attribute = event.getPlayer().getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(0);
            final NamespacedKey key = NamespacedKey.fromString("betterpvp:mining");
            attribute.addTransientModifier(new AttributeModifier(
                    key,
                    -Integer.MAX_VALUE,
                    AttributeModifier.Operation.ADD_NUMBER
            ));
        }
    }

    // ─── Animation helpers ───────────────────────────────────────────────────────

    private void clearOverlay(BreakSession session) {
        final World world = Bukkit.getWorld(session.getWorldUid());
        if (world == null) return;
        broadcastAnimation(world, session.getBlockPos(), session.getAnimationEntityId(), (byte) -1);
    }

    private void broadcastAnimation(World world, Vector3i pos, int entityId, byte stage) {
        final WrapperPlayServerBlockBreakAnimation packet =
                new WrapperPlayServerBlockBreakAnimation(entityId, pos, stage);

        final double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        for (Player viewer : world.getPlayers()) {
            final Location l = viewer.getLocation();
            final double dx = l.getX() - cx, dy = l.getY() - cy, dz = l.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz <= VIEW_RADIUS_SQ) {
                PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(packet);
            }
        }
    }

    // ─── PacketEvents listener (netty thread) ────────────────────────────────────

    private final class DigListener implements PacketListener {
        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;
            if (!(event.getPlayer() instanceof Player player)) return;

            final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            final DiggingAction action = packet.getAction();
            final Vector3i pos = packet.getBlockPosition();
            final UUID worldUid = player.getWorld().getUID();
            final UUID playerId = player.getUniqueId();

            switch (action) {
                case START_DIGGING -> {
                    final BlockFace face = toBukkitFace(packet.getBlockFace());

                    if (tryPredictInstantBreak(player, packet.getSequence(), worldUid, pos, face)) {
                        // Visual already flipped to AIR client-side from this thread;
                        // authoritative break is scheduled. Skip session creation.
                        return;
                    }

                    event.setCancelled(true);
                    ackAndResync(player, packet.getSequence(), pos);
                    startSession(player, worldUid, pos.getX(), pos.getY(), pos.getZ());
                    Bukkit.getScheduler().runTask(
                            JavaPlugin.getProvidingPlugin(BlockBreakProgressServiceImpl.class),
                            () -> dispatchBlockDamage(player, pos, face));
                }
                case CANCELLED_DIGGING -> {
                    event.setCancelled(true);
                    ackAndResync(player, packet.getSequence(), pos);
                    cancelSessionFor(playerId);
                }
                case FINISHED_DIGGING -> {
                    // Client thinks it broke the block; ack + resync forces it to snap back
                    // until our framework actually completes the break server-side.
                    event.setCancelled(true);
                    ackAndResync(player, packet.getSequence(), pos);
                }
                default -> { /* SWAP_ITEM, DROP_ITEM, RELEASE_USE_ITEM, etc. — leave alone */ }
            }
        }
    }

    /**
     * Main-thread companion to START_DIGGING: fires {@link BlockDamageEvent} and
     * honors {@code instaBreak} by completing the break right away. Skips work if
     * the player already cancelled the session before this scheduled task ran.
     */
    private void dispatchBlockDamage(Player player, Vector3i pos, BlockFace face) {
        final BreakSession session = sessions.get(player.getUniqueId());
        if (session == null) return; // already cancelled
        if (session.getBlockPos().getX() != pos.getX()
                || session.getBlockPos().getY() != pos.getY()
                || session.getBlockPos().getZ() != pos.getZ()) {
            return; // player retargeted between schedule and run
        }

        final World world = Bukkit.getWorld(session.getWorldUid());
        if (world == null) return;
        final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
        final ItemStack held = player.getInventory().getItemInMainHand();

        final BlockDamageEvent damage = new BlockDamageEvent(player, block, face, held, false);
        Bukkit.getPluginManager().callEvent(damage);

        if (damage.isCancelled()) {
            cancelSessionFor(player.getUniqueId());
            return;
        }

        if (damage.getInstaBreak()) {
            completeBreak(player, block, session, true);
        }
    }

    /**
     * Netty-thread fast path for blocks that are guaranteed to break on the very first tick.
     * <p>
     * Detection is done off-thread by reading {@link Block#getType()} (same risk profile as
     * {@link #ackAndResync}) and {@link Player#getGameMode()} (cached, safe). If the block
     * qualifies, we immediately:
     * <ol>
     *   <li>Ack the prediction sequence so the client stops floating it.</li>
     *   <li>Send a {@link WrapperPlayServerBlockChange} with AIR — the client renders the
     *       break in the next frame, no server-tick wait.</li>
     * </ol>
     * Then schedule the authoritative break on the main thread via
     * {@link #performPredictedBreak}, which fires {@link BlockDamageEvent} and runs
     * {@code breakNaturally}. If anything rejects the break (cancelled event, protection
     * plugin, etc.) the visual is restored.
     *
     * @return {@code true} if a prediction was made and the caller should skip its normal flow.
     */
    private boolean tryPredictInstantBreak(Player player, int sequence, UUID worldUid,
                                           Vector3i pos, BlockFace face) {

        final Material blockType;
        try {
            blockType = player.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getType();
        } catch (Throwable t) {
            return false; // off-thread race; fall back to tick-bound flow
        }

        final ItemStack held = player.getInventory().getItemInMainHand();
        final boolean predict = isCreativeInstant(player, held);
//        final boolean predict = blockType.getHardness() == 0f || isCreativeInstant(player, held);
        if (!predict) return false;

        // Visual: ack + flip to AIR client-side immediately.
        final var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        user.sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(sequence));
        try {
            user.sendPacket(new WrapperPlayServerBlockChange(pos,
                    SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(Material.AIR))));
        } catch (Throwable ignored) {
            // If air block-data construction somehow fails, abort prediction; caller falls back.
            return false;
        }

        // Any in-progress dig is invalidated by this fresh start.
        cancelSessionFor(player.getUniqueId());

        // Authoritative break next tick. The session map stays empty for this interaction —
        // there's no progress to accumulate.
        Bukkit.getScheduler().runTask(
                JavaPlugin.getProvidingPlugin(BlockBreakProgressServiceImpl.class),
                () -> performPredictedBreak(player, worldUid, pos, face));
        return true;
    }

    /**
     * Main-thread authoritative completion for a netty-thread prediction. Re-validates that the
     * break is actually allowed (the world might have changed, listeners might cancel) and either
     * commits the break or restores the client's view of the original block state.
     */
    private void performPredictedBreak(Player player, UUID worldUid, Vector3i pos, BlockFace face) {
        final World world = Bukkit.getWorld(worldUid);
        if (world == null) {
            return; // can't restore anyway; client will resync on chunk reload
        }
        final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());

        // If the block already changed (e.g. another player broke it, falling block landed),
        // just send the current state to correct any drift from the speculative AIR.
        if (block.getType().isAir()) {
            // Already air server-side — our prediction matched reality, nothing to do.
            nextAllowedProgressMillis.remove(player.getUniqueId());
            return;
        }

        final ItemStack held = player.getInventory().getItemInMainHand();
        final BlockDamageEvent damage = new BlockDamageEvent(player, block, face, held, true);
        Bukkit.getPluginManager().callEvent(damage);

        if (damage.isCancelled()) {
            sendBlockState(player, block);
            return;
        }

        if (!breakBlock(player, block)) {
            sendBlockState(player, block);
            return;
        }

        // Successful instant break: clear any cooldown, cancel anyone else digging this block.
        nextAllowedProgressMillis.remove(player.getUniqueId());
        cancelSessionsAt(world.getUID(), pos.getX(), pos.getY(), pos.getZ());
    }

    /** Re-send the authoritative block state to a single player; used to revert a failed prediction. */
    private void sendBlockState(Player player, Block block) {
        final var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        user.sendPacket(new WrapperPlayServerBlockChange(
                new Vector3i(block.getX(), block.getY(), block.getZ()),
                SpigotConversionUtil.fromBukkitBlockData(block.getBlockData())));
    }

    /**
     * Closes the modern-protocol prediction loop for a cancelled dig packet,
     * <b>fully on the netty thread</b> for zero perceptual delay:
     * <ol>
     *   <li>{@link WrapperPlayServerAcknowledgeBlockChanges} — settles the
     *       client's prediction sequence so it stops floating unconfirmed.</li>
     *   <li>{@link WrapperPlayServerBlockChange} — re-asserts the current block
     *       state, interrupting any in-flight client mining animation/sound.</li>
     * </ol>
     * Reading {@code BlockData} off-thread isn't officially guaranteed safe in
     * Bukkit, but in Paper it pulls from final-ish chunk-array slots and the
     * only failure mode is racing a concurrent write to that exact position —
     * extremely unlikely here since we just received a dig packet targeting it.
     * If a read does fail, the catch swallows it and the next animation tick
     * still self-corrects via the overlay packet.
     */
    private void ackAndResync(Player player, int sequence, Vector3i pos) {
        final var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        user.sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(sequence));

        try {
            // this may need to go on the main thread because of getBlockAt
            final Block block = player.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            user.sendPacket(new WrapperPlayServerBlockChange(pos,
                    SpigotConversionUtil.fromBukkitBlockData(block.getBlockData())));
        } catch (Throwable ignored) {
            // Off-thread chunk read raced a write; skip resync this round.
        }
    }

    private static BlockFace toBukkitFace(com.github.retrooper.packetevents.protocol.world.BlockFace peFace) {
        if (peFace == null) return BlockFace.UP;
        return switch (peFace) {
            case DOWN -> BlockFace.DOWN;
            case UP -> BlockFace.UP;
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
            default -> BlockFace.SELF; // OTHER — no Bukkit equivalent
        };
    }
}
