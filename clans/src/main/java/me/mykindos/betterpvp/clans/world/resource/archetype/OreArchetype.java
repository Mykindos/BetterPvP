package me.mykindos.betterpvp.clans.world.resource.archetype;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.world.resource.DegradeChain;
import me.mykindos.betterpvp.clans.world.resource.ResourceArchetype;
import me.mykindos.betterpvp.clans.world.resource.ResourceLoot;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeManager;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeProp;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeSpeed;
import me.mykindos.betterpvp.clans.world.resource.BlockReplacementStore;
import me.mykindos.betterpvp.clans.world.resource.Respawn;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mining archetype: replaces the old Fields ore system with a region-scan + snapshot model (no database).
 * <p>
 * A node carries one or more {@link DegradeChain degrade chains} ({@code chains}). On activation the cuboid region
 * is scanned once: every block whose material is the first stage of a <i>resource</i> chain is snapshotted as a
 * respawning ore point. A resource chain is one whose first stage never appears as a later stage of any chain — so
 * {@code copper_ore → stone} and {@code diamond_ore → stone} are mined resources (snapshotted, respawn, drop loot),
 * while {@code stone → cobblestone → deepslate} is a pure erosion rule (stone is the matrix every ore degrades into, so
 * it is not snapshotted and never respawns).
 * <p>
 * Mining a block steps it to the next stage. Resolution prefers the block's <i>origin</i> chain (the resource it was
 * snapshotted as) when that chain still continues from the current material; otherwise it falls back to any chain that
 * can continue the current material. So a copper point degrades {@code copper_ore → stone} (copper chain), then
 * {@code stone → cobblestone → deepslate} (stone chain) once copper's chain is exhausted, while a block that was stone
 * to begin with follows the stone chain from the start. Mining an intact resource point rolls that chain's loot table
 * (or drops vanilla if none is set) and schedules a respawn to the original ore. An unbreakable terminal (e.g.
 * {@code deepslate}) can never be mined and has its {@code BlockDamageEvent} cancelled by {@link ResourceNodeManager}.
 * Carries the {@code fields} zone tag so {@code ClanManager#isFields} keeps working.
 */
@Singleton
@CustomLog
public class OreArchetype implements ResourceArchetype {

    private final ResourceLoot loot;
    private final ResourceNodeSpeed speed;
    private final BlockReplacementStore progressStore;
    private final Map<Integer, OreField> fields = new ConcurrentHashMap<>();

    @Inject
    public OreArchetype(@NotNull ResourceLoot loot, @NotNull ResourceNodeSpeed speed,
                        @NotNull BlockReplacementStore progressStore) {
        this.loot = loot;
        this.speed = speed;
        this.progressStore = progressStore;
    }

    @Override
    public @NotNull String id() {
        return "ore";
    }

    @Override
    public @NotNull Set<String> zoneTags() {
        return Set.of(ClanZones.FIELDS);
    }

    @Override
    public void onActivate(@NotNull ResourceNodeProp node) {
        final List<DegradeChain> chains = parseChains(node.getDefinition().getRoot());
        if (chains.isEmpty()) {
            log.warn("Ore node '{}' has no valid 'chains' - skipping snapshot", node.getDefinition().getId()).submit();
            return;
        }

        final OreField field = new OreField(chains);
        // Snapshot only resource first-stages (materials that are never something another chain degrades into).
        final Map<Material, DegradeChain> snapshotMaterials = resourceMaterials(chains);
        final CuboidRegion region = (CuboidRegion) node.getRegion();
        final Location min = region.getMin();
        final Location max = region.getMax();
        final World world = min.getWorld();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    final DegradeChain origin = snapshotMaterials.get(block.getType());
                    if (origin != null) {
                        field.orePoints.put(pack(x, y, z), new OrePoint(x, y, z, block.getBlockData(), origin));
                    }
                }
            }
        }
        // Reconcile points that were mid-respawn when the server last stopped (graceful or crash): their world block
        // may still be degraded, so the scan above missed or mis-typed them. Re-create each as an inactive point
        // resuming its original timer; the next tick restores the block to its original and prunes the entry. Drop
        // entries whose material is no longer a resource head (the field's config changed under them).
        for (BlockReplacementStore.Entry entry : progressStore.entriesWithin(world, min, max)) {
            final BlockData original;
            try {
                original = Bukkit.createBlockData(entry.getBlockData());
            } catch (IllegalArgumentException invalid) {
                progressStore.clear(entry.getWorld(), entry.getX(), entry.getY(), entry.getZ());
                continue;
            }
            final DegradeChain origin = snapshotMaterials.get(original.getMaterial());
            if (origin == null) {
                progressStore.clear(entry.getWorld(), entry.getX(), entry.getY(), entry.getZ());
                continue;
            }
            final OrePoint point = new OrePoint(entry.getX(), entry.getY(), entry.getZ(), original, origin);
            point.active = false;
            point.lastMinedMs = entry.getMinedAtMs();
            field.orePoints.put(pack(entry.getX(), entry.getY(), entry.getZ()), point);
        }

        fields.put(node.getId(), field);
        log.info("Ore node '{}' snapshotted {} ore point(s) across {} chain(s)",
                node.getDefinition().getId(), field.orePoints.size(), chains.size()).submit();
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
        final OreField field = fields.get(node.getId());
        if (field == null) {
            return false;
        }

        final String current = DegradeChain.normalise(block.getType().getKey().getKey());
        final OrePoint point = field.orePoints.get(pack(block.getX(), block.getY(), block.getZ()));
        final DegradeChain origin = point != null ? point.chain : null;
        final DegradeChain governing = field.resolve(current, origin);
        if (governing == null) {
            return false; // terminal (possibly unbreakable) or unknown stage — leave the deny in place
        }
        final DegradeChain.Stage stage = governing.stageOf(current).orElseThrow();
        if (stage.unbreakable()) {
            return false; // un-mineable stage — block damage is cancelled separately, nothing to harvest
        }
        final Material nextMaterial = Material.matchMaterial(governing.next(current).orElseThrow());
        if (nextMaterial == null) {
            return false;
        }

        // Capture vanilla drops before the block is replaced (used only when this stage names no loot table).
        final List<ItemStack> vanillaDrops = stage.lootTable() == null
                ? new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand(), player))
                : List.of();

        // change it
        UtilBlock.playBlockEffect(block, block.getBlockData());
        block.setType(nextMaterial, false);

        // drop
        Location dropLocation = block.getLocation().toCenterLocation();
        final AttributeInstance attribute = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE));
        final RayTraceResult result = player.rayTraceBlocks(attribute.getValue());
        if (result != null) {
            dropLocation = result.getHitPosition().toLocation(block.getWorld());
            final Vector direction = player.getLocation().getDirection();
            dropLocation.subtract(direction.multiply(0.5)); // drop slightly closer to the player than the hit block's center
        }

        if (stage.lootTable() != null) {
            loot.award(stage.lootTable(), player, dropLocation);
        } else {
            final World world = block.getWorld();
            for (ItemStack drop : vanillaDrops) {
                world.dropItemNaturally(dropLocation, drop);
            }
        }

        // A snapshotted resource point mined from its intact state schedules a respawn back to its original ore, and is
        // persisted so it survives a restart mid-respawn (and the next scan does not mis-read the degraded block).
        if (point != null && point.active && current.equals(point.chain.first())) {
            point.lastMinedMs = System.currentTimeMillis();
            point.active = false;
            progressStore.markDirty(block.getWorld().getName(), point.x, point.y, point.z,
                    point.original.getAsString(), point.lastMinedMs);
        }
        return true;
    }

    @Override
    public boolean isUnbreakable(@NotNull ResourceNodeProp node, @NotNull Block block) {
        final OreField field = fields.get(node.getId());
        return field != null && field.unbreakableMaterials.contains(DegradeChain.normalise(block.getType().getKey().getKey()));
    }

    @Override
    public void tick(@NotNull ResourceNodeProp node) {
        final OreField field = fields.get(node.getId());
        if (field == null) {
            return;
        }

        final long now = System.currentTimeMillis();
        final double respawn = node.getDefinition().getRespawnSeconds();
        for (OrePoint point : field.orePoints.values()) {
            if (point.active) {
                continue;
            }
            if (!Respawn.isReady(point.lastMinedMs, respawn, speed.getBonusMultiplier(), now)) {
                continue;
            }
            final World world = node.getRegion().getWorld();
            if (world == null) {
                continue;
            }
            world.getBlockAt(point.x, point.y, point.z).setBlockData(point.original, false);
            point.active = true;
            progressStore.clear(world.getName(), point.x, point.y, point.z);
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /**
     * Parses {@code chains} (a map of named chains; each value is a list of stages, and each stage is either a plain
     * material string or an object with {@code material}, an optional {@code lootTable}, and an optional
     * {@code unbreakable} flag). The chain key is a readable label only and carries no behaviour. Chains shorter than
     * two stages are dropped (nothing to degrade to).
     */
    private static @NotNull List<DegradeChain> parseChains(@Nullable ConfigurationSection root) {
        final List<DegradeChain> chains = new ArrayList<>();
        if (root == null) {
            return chains;
        }
        final ConfigurationSection chainsSection = root.getConfigurationSection("chains");
        if (chainsSection == null) {
            return chains;
        }
        for (String name : chainsSection.getKeys(false)) {
            final DegradeChain chain = parseChain(chainsSection.getList(name));
            if (chain != null) {
                chains.add(chain);
            }
        }
        return chains;
    }

    private static @Nullable DegradeChain parseChain(@Nullable Object entry) {
        if (!(entry instanceof List<?> list) || list.size() < 2) {
            return null;
        }
        final List<DegradeChain.Stage> stages = new ArrayList<>(list.size());
        for (Object element : list) {
            stages.add(parseStage(element));
        }
        return DegradeChain.of(stages);
    }

    private static @NotNull DegradeChain.Stage parseStage(@Nullable Object element) {
        if (element instanceof Map<?, ?> map) {
            final Object lootTable = map.get("lootTable");
            return new DegradeChain.Stage(String.valueOf(map.get("material")),
                    lootTable == null ? null : lootTable.toString(),
                    Boolean.TRUE.equals(map.get("unbreakable")));
        }
        return new DegradeChain.Stage(String.valueOf(element), null, false);
    }

    /**
     * Maps every chain's first (intact) material to that chain — each chain head is a <i>resource</i> that is
     * snapshotted and respawns. This includes a head that another chain also degrades into (e.g. {@code stone}, the head
     * of the erosion chain that {@code copper}/{@code diamond} step down to), so its blocks are snapshotted and respawn
     * rather than eroding permanently.
     */
    private static @NotNull Map<Material, DegradeChain> resourceMaterials(@NotNull List<DegradeChain> chains) {
        final Map<Material, DegradeChain> resources = new HashMap<>();
        for (DegradeChain chain : chains) {
            final Material material = Material.matchMaterial(chain.first());
            if (material != null) {
                resources.putIfAbsent(material, chain);
            }
        }
        return resources;
    }

    /** Per-node snapshot of the ore points inside the field, plus its degrade chains. */
    private static final class OreField {
        private final List<DegradeChain> chains;
        private final Set<String> unbreakableMaterials = new HashSet<>();
        private final Map<Long, OrePoint> orePoints = new HashMap<>();

        private OreField(List<DegradeChain> chains) {
            this.chains = chains;
            for (DegradeChain chain : chains) {
                for (DegradeChain.Stage stage : chain.stages()) {
                    if (stage.unbreakable()) {
                        unbreakableMaterials.add(stage.material());
                    }
                }
            }
        }

        /**
         * Picks the chain that governs the next step for {@code current}: the block's {@code origin} chain when it still
         * continues, otherwise the first chain that can continue {@code current} (case-by-case conflict resolution).
         *
         * @return the governing chain, or null if no chain continues from {@code current} (terminal/unknown)
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
    }

    /** A single snapshotted ore block: its original data, the chain it belongs to, plus respawn bookkeeping. */
    private static final class OrePoint {
        private final int x;
        private final int y;
        private final int z;
        private final BlockData original;
        private final DegradeChain chain;
        private long lastMinedMs;
        private boolean active = true;

        private OrePoint(int x, int y, int z, BlockData original, DegradeChain chain) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.original = original;
            this.chain = chain;
        }
    }
}
