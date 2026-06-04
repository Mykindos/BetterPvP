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
import me.mykindos.betterpvp.core.block.SmartBlockBreakOverride;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockOverrides;
import me.mykindos.betterpvp.core.framework.blockbreak.ToolMiningSpeed;
import me.mykindos.betterpvp.core.framework.blockbreak.resolver.BlockBreakResolver;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
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
import java.util.OptionalDouble;
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
    private final SmartBlockFactory smartBlockFactory;
    /** One active session per player; new dig replaces previous. */
    private final ConcurrentHashMap<UUID, BreakSession> sessions = new ConcurrentHashMap<>();
    /** Wall-clock millis at which the player's next non-instant break may begin accumulating progress. */
    private final ConcurrentHashMap<UUID, Long> nextAllowedProgressMillis = new ConcurrentHashMap<>();
    /**
     * Latest dig-action prediction sequence per player. Because we cancel the vanilla
     * {@code PLAYER_DIGGING} packet, the vanilla {@code ackBlockChangesUpTo} (fired by
     * {@code ServerGamePacketListenerImpl} right after {@code handleBlockBreakAction})
     * never runs. We replay it ourselves <i>after</i> the break so the client's
     * prediction settles against the post-break (air) state in one round trip,
     * exactly like vanilla — instead of reverting to the pre-break state first.
     */
    private final ConcurrentHashMap<UUID, Integer> lastDigSequence = new ConcurrentHashMap<>();

    /** Soft cap for {@link #instantVerdict}; cleared wholesale when exceeded (entries are cheap to rebuild). */
    private static final int INSTANT_CACHE_MAX = 2048;
    /**
     * Main-thread-written, netty-read verdict cache: was the last resolve of
     * {@code (block Material, tool Material)} an instant break? Lets the packet
     * thread flip the client to AIR immediately for repeats (mining a vein,
     * clearing similar blocks) without touching the main-thread-only resolver.
     * A {@code true} entry is always re-validated on the main thread before the
     * authoritative break commits, so a stale entry only costs a brief flicker
     * and self-heals on the next resolve. Keyed by packed Material ordinals.
     */
    private final ConcurrentHashMap<Long, Boolean> instantVerdict = new ConcurrentHashMap<>();

    /** Outcome of a main-thread instant re-check; see {@link #resolveInstant}. */
    private enum InstantDecision { INSTANT, NOT_INSTANT, UNBREAKABLE }

    /**
     * Vanilla blocks Nexo/Oraxen reuse as custom-block carriers. Their true break
     * behaviour lives behind the main-thread-only smart factory, so the netty
     * vanilla estimate ({@link #estimatesInstantVanilla}) must never guess for
     * them — they fall back to the cache/tick path instead.
     */
    private static final java.util.Set<Material> NEXO_CARRIER_MATERIALS = java.util.EnumSet.of(
            Material.BARRIER, Material.NOTE_BLOCK, Material.MUSHROOM_STEM,
            Material.BROWN_MUSHROOM_BLOCK, Material.RED_MUSHROOM_BLOCK,
            Material.TRIPWIRE, Material.CHORUS_PLANT, Material.CHORUS_FLOWER);

    @Inject
    public BlockBreakProgressServiceImpl(BlockBreakResolver resolver, SmartBlockFactory smartBlockFactory) {
        this.resolver = resolver;
        this.smartBlockFactory = smartBlockFactory;
        PacketEvents.getAPI().getEventManager().registerListener(new DigListener(), PacketListenerPriority.HIGH);
    }

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
        cancelSessionsAtInternal(worldUid, x, y, z, null);
    }

    @Override
    public void cancelSessionsAt(@NotNull UUID worldUid, int x, int y, int z, @NotNull UUID exceptPlayerId) {
        cancelSessionsAtInternal(worldUid, x, y, z, exceptPlayerId);
    }

    private void cancelSessionsAtInternal(UUID worldUid, int x, int y, int z, UUID exceptPlayerId) {
        final Iterator<Map.Entry<UUID, BreakSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<UUID, BreakSession> entry = it.next();
            if (exceptPlayerId != null && exceptPlayerId.equals(entry.getKey())) continue;
            final BreakSession s = entry.getValue();
            if (s.getWorldUid().equals(worldUid)
                    && s.getBlockPos().getX() == x
                    && s.getBlockPos().getY() == y
                    && s.getBlockPos().getZ() == z) {
                clearOverlay(s);
                it.remove();
            }
        }
    }

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
            lastDigSequence.remove(session.getPlayerId());
            return;
        }

        if(player.getGameMode() == GameMode.ADVENTURE) {
            cancelSessionFor(session.getPlayerId());
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

        // Resolve the merged SmartBlock override (own-fields ∘ Nexo Breakable defaults),
        // breakability/speed, and smart-block status. These depend only on the block's
        // BlockData (the held item already ends the session on change), so we memoize
        // them on the session and recompute only when the BlockData changes — Vein Echo
        // respawn, falling block landing, turning to AIR, etc. Without this the whole
        // chain (incl. an O(n) Oraxen registry scan) ran every tick per dig.
        // Nexo blocks often appear as AIR or BARRIER, so we must not rely on getType().
        final BlockData blockData = block.getBlockData();
        if (!session.isResolvedFor(blockData)) {
            // The block under an active session changed (a degrade-chain step via setType, a Vein Echo respawn,
            // a falling block landing, etc.). When this isn't the session's first resolve, the dig is effectively
            // starting on a new block in place, so re-fire BlockDamageEvent — listeners that gate the *start* of
            // mining (block protection, unbreakable resource-node stages) get the same chance to cancel they would
            // on a fresh dig. A cancel ends the session; otherwise the destruction progress restarts from zero.
            final boolean blockChanged = session.getResolvedFor() != null && !block.getType().isAir();
            final SmartBlockBreakOverride resolvedOverride =
                    SmartBlockOverrides.resolve(smartBlockFactory, block, player, held);
            final boolean smart = !block.getType().isAir() && smartBlockFactory.isSmartBlock(block);
            session.cacheResolve(blockData, resolvedOverride, smart);
            if (blockChanged) {
                final BlockDamageEvent damage = new BlockDamageEvent(player, block, session.getFace(), held, false);
                Bukkit.getPluginManager().callEvent(damage);
                if (damage.isCancelled()) {
                    cancelSessionFor(session.getPlayerId());
                    return;
                }
                session.setProgress(0.0);
                session.setLastStageSent(-1);
                clearOverlay(session);
            }
        }

        final SmartBlockBreakOverride smartOverride = session.getCachedSmartOverride();
        final OptionalDouble smartHardness = smartOverride.hardness();

        if (smartHardness.isEmpty() && block.getType().isAir()) {
            return;
        }

        // Lazily resolve props *after* the air guard so its ordering (and any global
        // rule that matches AIR) is preserved exactly. Cached per BlockData alongside
        // the override; cleared whenever the override cache is recomputed.
        BlockBreakProperties props = session.getCachedProps();
        if (props == null) {
            props = resolver.resolve(player, block, held);
            session.setCachedProps(props);
        }
        if (!props.isBreakable()) {
            cancelSessionFor(session.getPlayerId());
            return;
        }

        final float hardness = smartHardness.isPresent()
                ? (float) smartHardness.getAsDouble()
                : block.getType().getHardness();
        if (hardness < 0f) {
            cancelSessionFor(session.getPlayerId());
            return;
        }

        // Unconditional instant breaks (Wiki "Breaking → Instant breaking"):
        //   - Hardness 0 blocks (grass, flowers, fire, etc.) — speed/0 = ∞ trumps any penalty.
        //   - Creative mode, except with non-mining items the wiki lists explicitly.
        // Bypass progress accumulation entirely so we don't leave a session that drips progress
        // when the client doesn't continuously send digging packets (creative single-click).
        final boolean allowed = nextAllowedProgressMillis.getOrDefault(session.getPlayerId(), 0L) <= System.currentTimeMillis();
        if (hardness == 0f || isCreativeInstant(player, held)) {
            if (!session.isCachedIsSmartBlock()) {
                recordInstantVerdict(block.getType(), held.getType(), true);
            }
            if (allowed) completeBreak(player, block, session, true);
            return;
        }

        final double basePerTick =
                (double) props.getBreakSpeed() / (ToolMiningSpeed.VANILLA_TICK_DIVISOR * hardness);

        // Player-state modifiers are multiplicative on damage per tick (Wiki "Breaking → Calculation").
        final double progressPerTick = basePerTick * playerStateMultiplier(player);

        // Wiki "Breaking": damage ≥ 1 in a single tick = instant break. progressPerTick is exactly that
        // damage value in our scaled units, so this check is equivalent to speedMultiplier > 30 * hardness.
        final boolean instant = progressPerTick >= 1.0;

        // Seed the netty-readable verdict cache from the authoritative resolve so a
        // repeat of this (block, tool) can be predicted instantly on the packet
        // thread. Storing the false case too lets a no-longer-instant key (lost
        // Haste, swapped tool tier) stop being mispredicted after one block.
        if (!session.isCachedIsSmartBlock()) {
            recordInstantVerdict(block.getType(), held.getType(), instant);
        }

        // Vanilla 6-tick post-completion delay. Successful instant breaks don't arm the
        // cooldown (so chains run freely), but a failed instant break does — the gate then
        // throttles the retry loop on listener-cancelled breaks.
        final Long gateUntil = nextAllowedProgressMillis.get(session.getPlayerId());
        if (gateUntil != null && System.currentTimeMillis() < gateUntil) {
            return;
        }

        session.setProgress(session.getProgress() + progressPerTick);

        // Provider-specific break-progress HUD (action bar overlay for Nexo/Oraxen blocks;
        // no-op for vanilla, which already shows the destruction-stage overlay).
        if (session.isCachedIsSmartBlock()) {
            smartBlockFactory.displayBreakProgress(player, block, Math.min(1.0, session.getProgress()));
        }

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

    /**
     * Reset the session's progress after a break attempt. The session is <b>not</b> removed —
     * end-of-session is driven by:
     * <ul>
     *   <li>{@code CANCELLED_DIGGING} packet (player released or retargeted)</li>
     *   <li>{@link #onItemHeldChange} / {@link #onQuit}</li>
     *   <li>A fresh {@code START_DIGGING} replacing this session</li>
     *   <li>The resolver returning {@code !isBreakable()} on the next tick (e.g. block is now AIR)</li>
     * </ul>
     * This means a held left-click survives across:
     * <ul>
     *   <li>A successful break followed by a same-tick block replacement (e.g. Vein Echo respawn)</li>
     *   <li>A {@code BlockBreakEvent} cancelled by a listener (e.g. Clans territory check) —
     *       progress simply restarts from 0 on the next tick</li>
     * </ul>
     * Because we pin {@code BLOCK_BREAK_SPEED} to {@code -Integer.MAX_VALUE} on join, the client
     * never emits {@code FINISHED_DIGGING} on its own and stays in "digging" state until it sees
     * a release/retarget — so it's safe to keep the session alive without producing duplicate
     * client-side break attempts.
     */
    private void completeBreak(Player player, Block block, BreakSession session, boolean instant) {
        final boolean targetSmartBlock = smartBlockFactory.isSmartBlock(block);
        session.setProgress(0.0);
        session.setLastStageSent(-1);
        clearOverlay(session);

        // Idempotency guard: tick() and the runTask-scheduled dispatchBlockDamage can both
        // try to complete the same instant break on the same tick. Whoever runs first
        // stamps the tick; the second call bails. Tick-stamp instead of an AIR check so
        // a same-tick replacement (e.g. Vein Echo restoring the block) isn't misread as
        // a fresh breakable block.
        if (block.getType().isAir() && !targetSmartBlock) {
            return;
        }

        final int sequence = lastDigSequence.getOrDefault(session.getPlayerId(), -1);
        final boolean broke = breakBlock(player, block, targetSmartBlock, sequence);
        if (!instant || !broke) {
            nextAllowedProgressMillis.put(
                    session.getPlayerId(),
                    System.currentTimeMillis() + INTER_BREAK_COOLDOWN_MS);
        } else {
            nextAllowedProgressMillis.remove(session.getPlayerId());
        }

        if (broke) {
            // Clear other players' sessions on this block, but keep ours alive so the
            // resolver can either (a) end it next tick when the block is AIR, or
            // (b) seamlessly continue if something replaces the block first.
            cancelSessionsAt(block.getWorld().getUID(),
                    block.getX(),
                    block.getY(),
                    block.getZ(),
                    player.getUniqueId());
        }
    }

    /**
     * Performs the authoritative break and then closes the modern-protocol
     * prediction loop the way vanilla does: world state is made authoritative
     * <i>first</i>, then the dig sequence is acked. Because the block is already
     * air (or restored, on a rejected break) when the client processes the ack,
     * the client's predicted state is confirmed in a single round trip with no
     * pre-break revert — the fix for the latency-scaled delay/flash.
     *
     * <p>We keep {@link UtilBlock#breakBlock} (which wraps
     * {@code ServerPlayerGameMode.destroyBlock} <i>and</i> emits the {@code 2001}
     * break particle/sound that {@code destroyBlock} alone omits) rather than
     * calling NMS destroy directly, so no break feedback is lost.
     */
    private boolean breakBlock(Player player, Block block, boolean smartBlock, int sequence) {
        final boolean broke;
        if (smartBlock) {
            final SmartBlockInstance smartBlockInstance = smartBlockFactory.from(block).orElseThrow();
            broke = smartBlockFactory.breakBlock(player, smartBlockInstance);
        } else {
            broke = UtilBlock.breakBlock(player, block);
        }

        final ServerPlayer handle = ((CraftPlayer) player).getHandle();
        if (!broke) {
            // Break rejected (protection plugin, cancelled BlockBreakEvent, plugin
            // set air, etc.) — re-assert the real block state before we ack so the
            // client reconciles to truth rather than a phantom air.
            handle.connection.send(new ClientboundBlockUpdatePacket(
                    handle.level(), new BlockPos(block.getX(), block.getY(), block.getZ())));
        }
        ackSequence(player, sequence);
        return broke;
    }

    /**
     * Vanilla's {@code ServerGamePacketListenerImpl#ackBlockChangesUpTo}: records
     * the highest settled prediction sequence; the connection flushes a single
     * {@code ClientboundBlockChangedAckPacket} for it at end of tick. Negative
     * sequences are illegal in vanilla (they disconnect the player), so we skip
     * acking when we never observed a real sequence.
     */
    private void ackSequence(Player player, int sequence) {
        if (sequence < 0) return;
        ((CraftPlayer) player).getHandle().connection.ackBlockChangesUpTo(sequence);
    }

    /** Re-assert the authoritative block state to one player and settle their prediction sequence. */
    private void restoreAndAck(Player player, Block block, int sequence) {
        final ServerPlayer handle = ((CraftPlayer) player).getHandle();
        handle.connection.send(new ClientboundBlockUpdatePacket(
                handle.level(), new BlockPos(block.getX(), block.getY(), block.getZ())));
        ackSequence(player, sequence);
    }

    /** Packs two {@link Material} ordinals into a long so the netty lookup allocates nothing. */
    private static long instantKey(Material block, Material tool) {
        return ((long) block.ordinal() << 32) | (tool.ordinal() & 0xffffffffL);
    }

    private void recordInstantVerdict(Material block, Material tool, boolean instant) {
        if (block.isAir()) return;
        if (instantVerdict.size() >= INSTANT_CACHE_MAX) instantVerdict.clear();
        instantVerdict.put(instantKey(block, tool), instant);
    }

    /**
     * The cached main-thread verdict for this (block, tool) pair, or {@code null}
     * if it has never been resolved. A non-null value is authoritative and
     * <i>overrides</i> the off-thread vanilla estimate — so once a pair is known
     * to be non-instant (custom rule/tool the estimate can't see) the estimate
     * stops re-triggering a flicker every hit.
     */
    private Boolean cachedInstantVerdict(Material block, Material tool) {
        return instantVerdict.get(instantKey(block, tool));
    }

    /**
     * Main-thread authoritative answer to "would this break instantly right now?".
     * Mirrors the resolve/instant math in {@link #tickOne} exactly (kept in sync by
     * hand — both compute {@code progressPerTick = breakSpeed / (VANILLA_TICK_DIVISOR
     * * hardness) * playerStateMultiplier}); used to re-validate a netty cache
     * prediction before the irreversible break, and to re-seed the cache so a stale
     * entry self-heals. Safe to call the resolver/smart-block factory here — this
     * only runs on the main thread.
     */
    private InstantDecision resolveInstant(Player player, Block block, ItemStack held) {
        final SmartBlockBreakOverride smartOverride =
                SmartBlockOverrides.resolve(smartBlockFactory, block, player, held);
        final boolean smart = !block.getType().isAir() && smartBlockFactory.isSmartBlock(block);
        final OptionalDouble smartHardness = smartOverride.hardness();

        if (smartHardness.isEmpty() && block.getType().isAir()) {
            return InstantDecision.UNBREAKABLE;
        }

        final BlockBreakProperties props = resolver.resolve(player, block, held);
        if (!props.isBreakable()) {
            return InstantDecision.UNBREAKABLE;
        }

        final float hardness = smartHardness.isPresent()
                ? (float) smartHardness.getAsDouble()
                : block.getType().getHardness();
        if (hardness < 0f) {
            return InstantDecision.UNBREAKABLE;
        }

        if (hardness == 0f || isCreativeInstant(player, held)) {
            if (!smart) recordInstantVerdict(block.getType(), held.getType(), true);
            return InstantDecision.INSTANT;
        }

        final double basePerTick =
                (double) props.getBreakSpeed() / (ToolMiningSpeed.VANILLA_TICK_DIVISOR * hardness);
        final boolean instant = basePerTick * playerStateMultiplier(player) >= 1.0;
        if (!smart) recordInstantVerdict(block.getType(), held.getType(), instant);
        return instant ? InstantDecision.INSTANT : InstantDecision.NOT_INSTANT;
    }

    /**
     * Cheap netty-thread estimate of "would this break instantly right now?",
     * mirroring the resolver's <i>vanilla-fallback</i> path
     * ({@code DefaultBlockBreakResolver#vanillaFallbackSpeed} folded into the
     * {@link #tickOne} progress formula) using only off-thread chunk reads — the
     * same accepted Paper risk as {@link #ackAndResync}. It deliberately cannot
     * see custom {@code ToolComponent}s, per-player global rules or smart-block
     * overrides (all main-thread-only), so it only ever guesses for plain
     * vanilla-speed breaks; everything else falls through to the cache/tick path
     * on first hit and is predicted thereafter. Every guess is re-validated by
     * {@link #resolveInstant} on the main thread before the break commits, which
     * restores + falls back on a miss and seeds {@link #instantVerdict}.
     *
     * <p>Caller wraps this in a {@code try/catch} (off-thread reads can race a
     * concurrent write); it does not catch internally.
     */
    private boolean estimatesInstantVanilla(Player player, Block block, Material blockType, ItemStack held) {
        if (NEXO_CARRIER_MATERIALS.contains(blockType)) return false;

        final float hardness = blockType.getHardness();
        if (hardness < 0f) return false; // unbreakable (bedrock, etc.)
        if (hardness == 0f) return true; // grass/flowers/fire — always instant

        float speed = block.getDestroySpeed(held); // vanilla tool speed vs this block
        if (!block.isPreferredTool(held)) {
            speed /= (100f / 30f); // vanilla "wrong tool for drops" speed penalty
        }
        final int scaled = Math.max(BlockBreakProperties.MIN_SPEED,
                Math.round(speed * ToolMiningSpeed.SCALE));
        final double progressPerTick = scaled
                / (ToolMiningSpeed.VANILLA_TICK_DIVISOR * hardness)
                * playerStateMultiplier(player);
        return progressPerTick >= 1.0;
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final UUID playerId = event.getPlayer().getUniqueId();
        cancelSessionFor(playerId);
        nextAllowedProgressMillis.remove(playerId);
        lastDigSequence.remove(playerId);
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

            // Track the client's prediction sequence for every dig action so the
            // eventual (possibly many-ticks-later) break can ack it. Sequences are
            // monotonic; acking the latest settles all earlier predictions too.
            if (packet.getSequence() >= 0) {
                lastDigSequence.put(playerId, packet.getSequence());
            }

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
        session.setFace(face); // remember for re-fired BlockDamageEvents when the block changes mid-session

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
            return;
        }

        // Collapse the second scheduler hop: instead of waiting for the next
        // @UpdateEvent tick to first touch this session, advance it once right
        // now. An instant break (progressPerTick ≥ 1) completes here — same tick
        // the dig was dispatched — instead of one tick later; a non-instant break
        // just gets its first progress increment early and continues normally.
        tickOne(session);
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
        if (nextAllowedProgressMillis.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) {
            return false; // still in cooldown from a previous non-instant break; can't predict
        }

        final Block block;
        final Material blockType;
        try {
            block = player.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            blockType = block.getType();
        } catch (Throwable t) {
            return false; // off-thread race; fall back to tick-bound flow
        }

        if (blockType.isAir()) return false; // nothing to break; also excludes Nexo-as-AIR

        final ItemStack held = player.getInventory().getItemInMainHand();
        // Three escalating predicates, cheapest first. All are re-validated by
        // resolveInstant in performPredictedBreak before the irreversible break, so
        // an optimistic guess costs at most a one-block flicker (then self-heals via
        // the cache) — never an unearned break or a wrong-speed mine:
        //   1. Creative — unconditionally instant.
        //   2. Cache hit — a prior main-thread resolve marked this (block,tool) instant.
        //   3. Off-thread vanilla estimate — makes the *first* break of a pair
        //      (e.g. leaves + shears) instant client-side without waiting for the
        //      cache to warm. Mirrors the resolver's vanilla-fallback path exactly.
        final boolean predict;
        if (isCreativeInstant(player, held)) {
            predict = true;
        } else {
            final Boolean cached = cachedInstantVerdict(blockType, held.getType());
            if (cached != null) {
                predict = cached; // authoritative; never second-guess with the estimate
            } else {
                try {
                    predict = estimatesInstantVanilla(player, block, blockType, held);
                } catch (Throwable ignored) {
                    return false; // off-thread chunk read raced a write; fall back to tick flow
                }
            }
        }
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
        final int sequence = lastDigSequence.getOrDefault(player.getUniqueId(), -1);

        // If the block already changed (e.g. another player broke it, falling block landed),
        // our prediction matched reality — just settle the sequence and bail.
        if (block.getType().isAir()) {
            nextAllowedProgressMillis.remove(player.getUniqueId());
            ackSequence(player, sequence);
            return;
        }

        final ItemStack held = player.getInventory().getItemInMainHand();

        // Authoritative re-validation. The netty predicate is a cheap cached guess;
        // a stale entry (e.g. player lost Haste since the cache was seeded) must NOT
        // be allowed to instant-break a block that should take time. resolveInstant
        // also re-seeds the cache, so a misprediction self-heals after one block.
        final InstantDecision decision = resolveInstant(player, block, held);
        if (decision == InstantDecision.UNBREAKABLE) {
            restoreAndAck(player, block, sequence);
            return;
        }
        if (decision == InstantDecision.NOT_INSTANT) {
            // Not actually instant — undo the speculative AIR and fall back to the
            // normal progress-driven session so the player mines it legitimately.
            restoreAndAck(player, block, sequence);
            startSession(player, worldUid, pos.getX(), pos.getY(), pos.getZ());
            Bukkit.getScheduler().runTask(
                    JavaPlugin.getProvidingPlugin(BlockBreakProgressServiceImpl.class),
                    () -> dispatchBlockDamage(player, pos, face));
            return;
        }

        final BlockDamageEvent damage = new BlockDamageEvent(player, block, face, held, true);
        Bukkit.getPluginManager().callEvent(damage);

        if (damage.isCancelled()) {
            restoreAndAck(player, block, sequence);
            return;
        }

        // breakBlock handles both the post-break ack and the fail-restore+ack.
        if (!breakBlock(player, block, smartBlockFactory.isSmartBlock(block), sequence)) {
            return;
        }

        // Successful instant break: clear any cooldown, cancel anyone else digging this block.
        nextAllowedProgressMillis.remove(player.getUniqueId());
        cancelSessionsAt(world.getUID(), pos.getX(), pos.getY(), pos.getZ());
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
