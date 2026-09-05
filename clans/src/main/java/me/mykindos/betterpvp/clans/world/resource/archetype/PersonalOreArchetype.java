package me.mykindos.betterpvp.clans.world.resource.archetype;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.resource.DegradeChain;
import me.mykindos.betterpvp.clans.world.resource.DegradeChains;
import me.mykindos.betterpvp.clans.world.resource.PersonalBlock;
import me.mykindos.betterpvp.clans.world.resource.PersonalMineRepository;
import me.mykindos.betterpvp.clans.world.resource.PersonalOreView;
import me.mykindos.betterpvp.clans.world.resource.ResourceArchetype;
import me.mykindos.betterpvp.clans.world.resource.ResourceLoot;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeProp;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeSpeed;
import me.mykindos.betterpvp.clans.world.resource.Respawn;
import me.mykindos.betterpvp.clans.world.resource.RespawnPoint;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mining archetype whose depletion is <b>per player</b>: everyone sees the node whole until they mine it themselves,
 * and each player's ore comes back on their own timer. Two people can stand in the same mine and see different walls.
 * <p>
 * This is what a starter mine needs and a contested one must not have. A shared field is a race - arrive late and it is
 * stripped - which is the right pressure in a field worth fighting over and the wrong first impression for someone who
 * has just logged in for the first time.
 *
 * <h3>The world block never changes</h3>
 * The block in the world stays pristine forever and is the shared origin every player's view is measured against. That
 * inverts the usual arrangement, and the consequence is the thing to keep in mind when reading this class:
 * <b>{@code block.getType()} is not the current stage</b>. It is always the intact ore. The stage a given player is
 * looking at lives in this node's per-player state, and every decision here reads it from there.
 * <p>
 * Because nothing in the world diverges, nothing can desync: two players mining the same block never contend for it,
 * and a chunk reload simply repaints from the origin. The cost is that the state has nowhere to live but a database -
 * see {@link PersonalMineRepository}.
 *
 * <h3>Where the divergence is applied</h3>
 * A player's view cannot be held by <i>racing</i> the server's block updates with {@link Player#sendBlockChange}: every
 * re-assert of world state lands after ours and wins - the break path being the worst case, since cancelling the
 * {@code BlockBreakEvent} is precisely what makes both CraftBukkit and the break service resend the intact ore. So the
 * divergence is applied at the packet seam instead, by {@link PersonalOreView}, which rewrites outgoing block and chunk
 * updates from the state below. This class owns the state and answers {@link #viewAt} / {@link #viewInChunk}; it sends
 * blocks itself only where nothing else will (the first paint after a load, and a point coming back on respawn).
 *
 * <h3>Loot</h3>
 * A stage with no {@code lootTable} drops <b>nothing</b>, where {@link OreArchetype} would fall back to the block's
 * vanilla drops. Vanilla drops are derived from the world block, which here is deliberately not what the player mined,
 * so falling back to them would hand out the intact ore however far down the chain the player actually was. Erosion
 * stages dropping nothing is what a curated node wants anyway.
 *
 * @see OreArchetype the shared-state counterpart
 */
@Singleton
@CustomLog
public class PersonalOreArchetype implements ResourceArchetype {

    private final Clans clans;
    private final ResourceLoot loot;
    private final ResourceNodeSpeed speed;
    private final PersonalMineRepository repository;
    private final ClientManager clientManager;
    private final Map<Integer, PersonalField> fields = new ConcurrentHashMap<>();

    @Inject
    public PersonalOreArchetype(@NotNull Clans clans, @NotNull ResourceLoot loot, @NotNull ResourceNodeSpeed speed,
                                @NotNull PersonalMineRepository repository, @NotNull ClientManager clientManager) {
        this.clans = clans;
        this.loot = loot;
        this.speed = speed;
        this.repository = repository;
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull String id() {
        return "personal_ore";
    }

    /**
     * Deliberately no {@code fields} tag. That tag marks a contested resource area, and a node whose ore nobody can
     * take from you is the opposite of one - a starter mine carrying it would inherit Fields' combat and drop rules in
     * what is meant to be the safest room on the server.
     */
    @Override
    public @NotNull Set<String> zoneTags() {
        return Set.of();
    }

    @Override
    public void onActivate(@NotNull ResourceNodeProp node) {
        final List<DegradeChain> chains = DegradeChains.parse(node.getDefinition().getRoot());
        if (chains.isEmpty()) {
            log.warn("Personal ore node '{}' has no valid 'chains' - skipping", node.getDefinition().getId()).submit();
            return;
        }
        if (node.getDefinition().isOneShot()) {
            // A one-shot personal node would have to keep every player's rows forever, which is what the housekeeping
            // prune exists to stop. Treated as a normal respawn rather than silently never coming back.
            log.warn("Personal ore node '{}' is one-shot, which this archetype does not support - using the respawn delay instead",
                    node.getDefinition().getId()).submit();
        }

        final Map<Material, DegradeChain> heads = DegradeChains.resourceMaterials(chains);
        final CuboidRegion region = (CuboidRegion) node.getRegion();
        final Location min = region.getMin();
        final Location max = region.getMax();
        final World world = min.getWorld();

        final PersonalField field = new PersonalField(world.getName(), node.getDefinition().getId(), chains);
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    final DegradeChain origin = heads.get(block.getType());
                    if (origin != null) {
                        field.origins.put(pack(x, y, z), new Origin(x, y, z, block.getBlockData(), origin));
                    }
                }
            }
        }

        fields.put(node.getId(), field);
        log.info("Personal ore node '{}' mapped {} ore point(s) across {} chain(s)",
                node.getDefinition().getId(), field.origins.size(), chains.size()).submit();
    }

    @Override
    public void onDeactivate(@NotNull ResourceNodeProp node) {
        fields.remove(node.getId());
    }

    @Override
    public boolean onHarvest(@NotNull ResourceNodeProp node, @NotNull Player player, @Nullable Block block,
                             @NotNull ZoneInteraction interaction) {
        if (block == null || interaction != ZoneInteraction.BREAK) {
            return false;
        }
        final PersonalField field = fields.get(node.getId());
        if (field == null) {
            return false;
        }
        final long key = pack(block.getX(), block.getY(), block.getZ());
        final Origin origin = field.origins.get(key);
        if (origin == null) {
            return false; // not one of this node's points - leave the deny in place
        }

        // This player's stage, not the world's: the world block is still the intact ore however far down this player
        // has already taken it.
        final Map<Long, Point> mine = field.byPlayer.get(player.getUniqueId());
        final Point existing = mine == null ? null : mine.get(key);
        final String current = existing == null ? origin.chain.first() : existing.stage;

        final DegradeChain governing = field.resolve(current, origin.chain);
        if (governing == null) {
            return false; // terminal or unknown stage
        }
        final DegradeChain.Stage stage = governing.stageOf(current).orElseThrow();
        if (stage.unbreakable()) {
            return false;
        }
        final String nextStage = governing.next(current).orElseThrow();
        final Material next = Material.matchMaterial(nextStage);
        if (next == null) {
            return false;
        }

        // Recorded before anything is sent, because the send is not what makes the new stage stick. Cancelling the
        // break makes both CraftBukkit and the break service re-assert the world block to this player as the frame
        // unwinds, and PersonalOreView rewrites those packets from this map - which has to already hold the new stage
        // by the time they go out.
        // The respawn is measured from the first break out of the intact state, so stepping an already-broken point
        // further down its chain keeps the original timestamp rather than restarting the clock.
        final long minedAt = existing == null ? System.currentTimeMillis() : existing.minedAtMs;
        field.byPlayer.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .put(key, new Point(nextStage, minedAt));

        // Shown to this player alone. Broadcasting either of these would announce the break to people for whom the
        // block never moved, and they would watch it shatter and stay standing.
        UtilBlock.playBlockEffect(player, block, existing == null ? origin.original : dataOf(current));
        player.sendBlockChange(block.getLocation(), next.createBlockData());

        if (stage.lootTable() != null) {
            loot.award(stage.lootTable(), player, ResourceLoot.dropLocation(player, block));
        }
        repository.save(clientId(player), field.world, field.node,
                new PersonalMineRepository.Point(origin.x, origin.y, origin.z, nextStage, minedAt));
        return true;
    }

    /**
     * A rule making this player's own exhausted points un-mineable. Registered per player, so the matcher only has to
     * look at the block - it already knows whose view it is answering for, which is what lets one block be finished for
     * one player and untouched for everyone else.
     */
    @Override
    public @NotNull List<BlockBreakRule> breakRules(@NotNull ResourceNodeProp node, @NotNull Player player) {
        final PersonalField field = fields.get(node.getId());
        if (field == null) {
            return List.of();
        }
        return List.of(BlockBreakRule.of(new ExhaustedPointMatcher(field, player.getUniqueId()),
                BlockBreakProperties.unbreakable()));
    }

    /** Per viewer by construction: a player is only ever told about the blocks they themselves have mined out. */
    @Override
    public boolean timersArePerPlayer() {
        return true;
    }

    @Override
    public @NotNull Collection<RespawnPoint> respawningPoints(@NotNull ResourceNodeProp node, @Nullable Player viewer) {
        final PersonalField field = fields.get(node.getId());
        if (field == null || viewer == null) {
            return List.of();
        }
        final Map<Long, Point> mine = field.byPlayer.get(viewer.getUniqueId());
        if (mine == null || mine.isEmpty()) {
            return List.of();
        }
        final double respawn = node.getDefinition().getRespawnSeconds();
        final double modifier = speed.getBonusMultiplier();
        final long total = Respawn.totalMs(respawn, modifier);
        final long now = System.currentTimeMillis();

        final List<RespawnPoint> points = new ArrayList<>();
        for (Map.Entry<Long, Point> entry : mine.entrySet()) {
            final Origin origin = field.origins.get(entry.getKey());
            if (origin == null || !origin.chain.showTimer()) {
                continue;
            }
            final long remaining = Respawn.remainingMs(entry.getValue().minedAtMs, respawn, modifier, now);
            if (remaining > 0) {
                points.add(new RespawnPoint(origin.x, origin.y, origin.z, remaining, total));
            }
        }
        return points;
    }

    @Override
    public void tick(@NotNull ResourceNodeProp node) {
        final PersonalField field = fields.get(node.getId());
        if (field == null) {
            return;
        }
        final double respawn = node.getDefinition().getRespawnSeconds();
        final long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<Long, Point>> entry : field.byPlayer.entrySet()) {
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue; // offline: their rows stand, and are handed back to them when they return
            }
            final Iterator<Map.Entry<Long, Point>> points = entry.getValue().entrySet().iterator();
            while (points.hasNext()) {
                final Map.Entry<Long, Point> point = points.next();
                if (!Respawn.isReady(point.getValue().minedAtMs, respawn, speed.getBonusMultiplier(), now)) {
                    continue;
                }
                points.remove();
                final Origin origin = field.origins.get(point.getKey());
                if (origin == null) {
                    continue;
                }
                player.sendBlockChange(new Location(player.getWorld(), origin.x, origin.y, origin.z), origin.original);
                repository.delete(clientId(player), field.world, origin.x, origin.y, origin.z);
            }
        }
    }

    /**
     * Loads what {@code player} had already mined here and paints it for them. Called when they enter the node, which
     * is the first moment their state can matter and, for the majority of players who never visit, a moment that never
     * arrives.
     */
    public void loadFor(@NotNull ResourceNodeProp node, @NotNull Player player) {
        final PersonalField field = fields.get(node.getId());
        if (field == null) {
            return;
        }
        final UUID playerId = player.getUniqueId();
        // Claim the slot up front: entering, leaving and re-entering while the query is still in flight would
        // otherwise load the same rows twice.
        if (field.byPlayer.putIfAbsent(playerId, new ConcurrentHashMap<>()) != null) {
            return;
        }

        final long client = clientId(player);
        final double respawn = node.getDefinition().getRespawnSeconds();
        repository.load(client, field.world, field.node).thenAccept(points -> UtilServer.runTask(clans, () -> {
            final Map<Long, Point> mine = field.byPlayer.get(playerId);
            if (mine == null || !player.isOnline()) {
                return; // left while the query was in flight
            }
            final long now = System.currentTimeMillis();
            for (PersonalMineRepository.Point stored : points) {
                final long key = pack(stored.getX(), stored.getY(), stored.getZ());
                if (!field.origins.containsKey(key)) {
                    // The node was re-authored under them and this block is no longer part of it.
                    repository.delete(client, field.world, stored.getX(), stored.getY(), stored.getZ());
                    continue;
                }
                if (Respawn.isReady(stored.getMinedAtMs(), respawn, speed.getBonusMultiplier(), now)) {
                    repository.delete(client, field.world, stored.getX(), stored.getY(), stored.getZ());
                    continue; // respawned while they were away
                }
                mine.put(key, new Point(stored.getStage(), stored.getMinedAtMs()));
            }
            sendView(player, field);
        }));
    }

    /** Drops a player's in-memory view. Their rows stand - this is a cache, the database is the record. */
    public void evict(@NotNull UUID playerId) {
        for (PersonalField field : fields.values()) {
            field.byPlayer.remove(playerId);
        }
    }

    /**
     * The stage {@code playerId} should be shown at one point, or null where they see the world's own block.
     * <p>
     * Asked by {@link PersonalOreView} on the netty thread for every outgoing block update on the server, so it does
     * no more than a couple of map lookups per loaded node and answers null without allocating.
     */
    public @Nullable String viewAt(@NotNull UUID playerId, @NotNull String world, int x, int y, int z) {
        final long key = pack(x, y, z);
        for (PersonalField field : fields.values()) {
            if (!field.world.equals(world) || !field.origins.containsKey(key)) {
                continue;
            }
            final Map<Long, Point> mine = field.byPlayer.get(playerId);
            final Point point = mine == null ? null : mine.get(key);
            if (point != null) {
                return point.stage;
            }
        }
        return null;
    }

    /**
     * Every point {@code playerId} has depleted inside one chunk column. Empty for very nearly every (player, chunk)
     * pair there is, which is what makes it cheap enough to ask on each chunk packet.
     */
    public @NotNull List<PersonalBlock> viewInChunk(@NotNull UUID playerId, @NotNull String world,
                                                    int chunkX, int chunkZ) {
        List<PersonalBlock> blocks = null;
        for (PersonalField field : fields.values()) {
            if (!field.world.equals(world)) {
                continue;
            }
            final Map<Long, Point> mine = field.byPlayer.get(playerId);
            if (mine == null || mine.isEmpty()) {
                continue;
            }
            for (Map.Entry<Long, Point> entry : mine.entrySet()) {
                final Origin origin = field.origins.get(entry.getKey());
                if (origin == null || origin.x >> 4 != chunkX || origin.z >> 4 != chunkZ) {
                    continue;
                }
                if (blocks == null) {
                    blocks = new ArrayList<>();
                }
                blocks.add(new PersonalBlock(origin.x, origin.y, origin.z, entry.getValue().stage));
            }
        }
        return blocks == null ? List.of() : blocks;
    }

    /**
     * Paints everything this player has depleted in one node. Only needed once, when their rows first arrive: the
     * client is already holding the pristine chunk at that moment and nothing will resend it on its own.
     */
    private void sendView(@NotNull Player player, @NotNull PersonalField field) {
        final Map<Long, Point> mine = field.byPlayer.get(player.getUniqueId());
        if (mine == null || mine.isEmpty() || !field.world.equals(player.getWorld().getName())) {
            return;
        }
        for (Map.Entry<Long, Point> entry : mine.entrySet()) {
            final Origin origin = field.origins.get(entry.getKey());
            if (origin == null) {
                continue;
            }
            final Material material = Material.matchMaterial(entry.getValue().stage);
            if (material != null) {
                player.sendBlockChange(new Location(player.getWorld(), origin.x, origin.y, origin.z),
                        material.createBlockData());
            }
        }
    }

    private long clientId(@NotNull Player player) {
        return clientManager.search().online(player).getId();
    }

    private static @NotNull BlockData dataOf(@NotNull String stage) {
        final Material material = Material.matchMaterial(stage);
        return material == null ? Material.STONE.createBlockData() : material.createBlockData();
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** The pristine state of one point: what the world holds there, and the chain it heads. */
    private static final class Origin {
        private final int x;
        private final int y;
        private final int z;
        private final BlockData original;
        private final DegradeChain chain;

        private Origin(int x, int y, int z, BlockData original, DegradeChain chain) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.original = original;
            this.chain = chain;
        }
    }

    /** How far one player has taken one point, and when they started it. */
    private static final class Point {
        private final String stage;
        private final long minedAtMs;

        private Point(String stage, long minedAtMs) {
            this.stage = stage;
            this.minedAtMs = minedAtMs;
        }
    }

    /** A node's pristine points and chains, plus every present player's divergence from them. */
    private static final class PersonalField {
        private final String world;
        private final String node;
        private final List<DegradeChain> chains;
        private final Set<String> unbreakableStages = new HashSet<>();
        private final Map<Long, Origin> origins = new ConcurrentHashMap<>();
        private final Map<UUID, Map<Long, Point>> byPlayer = new ConcurrentHashMap<>();

        private PersonalField(String world, String node, List<DegradeChain> chains) {
            this.world = world;
            this.node = node;
            this.chains = chains;
            for (DegradeChain chain : chains) {
                for (DegradeChain.Stage stage : chain.stages()) {
                    if (stage.unbreakable()) {
                        unbreakableStages.add(stage.material());
                    }
                }
            }
        }

        /**
         * Picks the chain governing the next step from {@code current}: the point's own chain while it still continues,
         * otherwise the first chain that can carry on from where it is.
         */
        private @Nullable DegradeChain resolve(@NotNull String current, @Nullable DegradeChain origin) {
            if (origin != null && origin.next(current).isPresent()) {
                return origin;
            }
            for (DegradeChain chain : chains) {
                if (chain.next(current).isPresent()) {
                    return chain;
                }
            }
            return null;
        }

        /** The stage {@code playerId} currently sees at a point, or null if the point is not one of this node's. */
        private @Nullable String stageFor(@NotNull UUID playerId, long key) {
            final Origin origin = origins.get(key);
            if (origin == null) {
                return null;
            }
            final Map<Long, Point> mine = byPlayer.get(playerId);
            final Point point = mine == null ? null : mine.get(key);
            return point == null ? origin.chain.first() : point.stage;
        }
    }

    /**
     * Matches the points this one player has already worked down to an unbreakable stage. Held in that player's rule
     * list, so an exhausted block stops accepting their digging while staying freshly mineable for everyone else.
     */
    private static final class ExhaustedPointMatcher implements BlockMatcher {

        private static final Set<Material> ALL_MATERIALS =
                Collections.unmodifiableSet(EnumSet.allOf(Material.class));

        private final PersonalField field;
        private final UUID playerId;

        private ExhaustedPointMatcher(PersonalField field, UUID playerId) {
            this.field = field;
            this.playerId = playerId;
        }

        @Override
        public boolean matches(@NotNull Block block) {
            final String stage = field.stageFor(playerId, pack(block.getX(), block.getY(), block.getZ()));
            return stage != null && field.unbreakableStages.contains(stage);
        }

        @Override
        public @NotNull Set<Material> knownMaterials() {
            return ALL_MATERIALS;
        }
    }
}
