package me.mykindos.betterpvp.core.block.oraxen;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockBreakOverride;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Factory class for creating {@link SmartBlockInstance}s using Oraxen
 */
@Singleton
public class OraxenSmartBlockFactory implements SmartBlockFactory {

    private final SmartBlockRegistry smartBlockRegistry;
    private final SmartBlockDataManager dataManager;
    private final ClientManager clientManager;

    /** Reverse index Oraxen-id → SmartBlock, rebuilt only when the registry grows. */
    private final Map<String, SmartBlock> oraxenIdIndex = new HashMap<>();
    private int indexedRegistrySize = -1;

    @Inject
    private OraxenSmartBlockFactory(SmartBlockRegistry smartBlockRegistry,
                                    SmartBlockDataManager dataManager,
                                    ClientManager clientManager) {
        this.smartBlockRegistry = smartBlockRegistry;
        this.dataManager = dataManager;
        this.clientManager = clientManager;
    }

    private SmartBlockInstance create(SmartBlock type, Location location, Mechanic mechanic) {
        if (mechanic instanceof FurnitureMechanic furnitureMechanic) {
            final Collection<ItemDisplay> nearby = location.getNearbyEntitiesByType(ItemDisplay.class, 4.0, 4.0, 4.0);
            ItemDisplay itemDisplay = null;
            double distance = Float.MAX_VALUE;
            for (ItemDisplay display : nearby) {
                if (!OraxenFurniture.isFurniture(display)) {
                    continue;
                }

                double newDistance = (float) display.getLocation().distanceSquared(location);
                if (newDistance < distance) {
                    itemDisplay = display;
                    distance = newDistance;
                }
            }

            if (itemDisplay == null) {
                throw new IllegalArgumentException("No ItemDisplay found for the given location: " + location);
            }

            location = itemDisplay.getLocation().clone();
        }

        return new SmartBlockInstance(type, location, dataManager);
    }

    public Optional<Mechanic> mechanic(Block block) {
        return Optional.ofNullable(OraxenBlocks.getOraxenBlock(block.getBlockData()))
                .or(() -> Optional.ofNullable(OraxenFurniture.getFurnitureMechanic(block)));
    }

    private SmartBlock getBlock(@NotNull String oraxenId) {
        final Map<String, SmartBlock> all = smartBlockRegistry.getAllBlocks();
        if (all.size() != indexedRegistrySize) {
            oraxenIdIndex.clear();
            for (SmartBlock block : all.values()) {
                if (block instanceof OraxenBlock oraxenBlock) {
                    oraxenIdIndex.putIfAbsent(oraxenBlock.getId(), block);
                }
            }
            indexedRegistrySize = all.size();
        }
        return oraxenIdIndex.get(oraxenId);
    }

    public Optional<Mechanic> mechanic(Entity entity) {
        return Optional.ofNullable(OraxenFurniture.getFurnitureMechanic(entity));
    }

    public Optional<SmartBlockInstance> from(Location location) {
        final Collection<ItemDisplay> nearby = location.getNearbyEntitiesByType(ItemDisplay.class, 1.0, 1.0, 1.0);
        for (ItemDisplay display : nearby) {
            if (OraxenFurniture.isFurniture(display)) {
                return from(display);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SmartBlockInstance> from(Block block) {
        return mechanic(block).map(mechanic -> {
            final SmartBlock smartBlock = getBlock(mechanic.getItemID());
            return create(smartBlock, block.getLocation(), mechanic);
        });
    }

    @Override
    public Optional<SmartBlockInstance> fromTarget(Player player) {
        final AttributeInstance attribute = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE));
        final double blockRange = attribute.getValue();
        final RayTraceResult rayTraceResult = player.rayTraceBlocks(blockRange);
        if (rayTraceResult != null) {
            final Block block = rayTraceResult.getHitBlock();
            // Block-class match first; thread-safe and avoids the entity scan when not needed.
            if (block != null && OraxenBlocks.isOraxenBlock(block)) {
                return from(block);
            }
        }

        // Furniture fallback — Oraxen has no findTargetFurniture helper, so use Bukkit's
        // entity ray-trace and filter to furniture displays.
        final Entity targetEntity = player.getTargetEntity((int) Math.ceil(blockRange));
        if (targetEntity instanceof ItemDisplay display && OraxenFurniture.isFurniture(display)) {
            return from(display);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SmartBlockInstance> load(Block block) {
        final Collection<ItemDisplay> entities = block.getLocation().getNearbyEntitiesByType(ItemDisplay.class, 2);
        if (entities.isEmpty()) return Optional.empty();
        return entities.stream().min((a, b) -> {
            double distA = a.getLocation().distanceSquared(block.getLocation());
            double distB = b.getLocation().distanceSquared(block.getLocation());
            return Double.compare(distA, distB);
        }).flatMap(this::from);
    }

    public Optional<SmartBlockInstance> from(Entity entity) {
        return mechanic(entity).map(mechanic -> {
            final SmartBlock smartBlock = getBlock(mechanic.getItemID());
            return create(smartBlock, entity.getLocation(), mechanic);
        });
    }

    @Override
    public boolean isSmartBlock(Block block) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This method must be called on the main thread");
        }
        return OraxenBlocks.isOraxenBlock(block) ||
                OraxenFurniture.getFurnitureMechanic(block) != null;
    }

    @Override
    public boolean isTargetSmartBlock(@NotNull Player player) {
        final AttributeInstance attribute = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE));
        final double blockRange = attribute.getValue();
        final RayTraceResult rayTraceResult = player.rayTraceBlocks(blockRange);
        if (rayTraceResult != null) {
            final Block block = rayTraceResult.getHitBlock();
            if (block != null && OraxenBlocks.isOraxenBlock(block)) {
                return true;
            }
        }
        final Entity targetEntity = player.getTargetEntity((int) Math.ceil(blockRange));
        return targetEntity instanceof ItemDisplay display && OraxenFurniture.isFurniture(display);
    }

    @Override
    public boolean isSmartBlock(Location location) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This method must be called on the main thread");
        }
        return from(location).isPresent();
    }

    @Override
    public boolean isSmartBlock(Entity entity) {
        final Block block = entity.getLocation().getBlock();
        return NexoBlocks.chorusBlockMechanic(block) != null ||
                NexoBlocks.customBlockMechanic(block) != null ||
                NexoBlocks.noteBlockMechanic(block) != null ||
                NexoBlocks.stringMechanic(block) != null ||
                NexoFurniture.furnitureMechanic(entity) != null;
    }

    @Override
    public BlockData createBlockData(SmartBlock type) {
        if (!(type instanceof OraxenBlock block)) {
            throw new IllegalArgumentException("Type must be an instance of OraxenBlock");
        }
        return OraxenBlocks.getOraxenBlockData(block.getId());
    }

    @Override
    public void displayBreakProgress(@NotNull Player player, @NotNull Block block, double progress) {
        final float clamped = (float) Math.max(0.0, Math.min(1.0, progress));
        final TextComponent bar = ProgressBar.withProgress(clamped)
                .withProgressColor(NamedTextColor.WHITE)
                .withRemainingColor(NamedTextColor.DARK_GRAY)
                .build();
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(500, TitleComponent.subtitle(0.0,
                0.1,
                0.0,
                false,
                g -> bar));
    }

    @Override
    public boolean breakBlock(Player player, SmartBlockInstance instance) {
        if (!(instance.getType() instanceof OraxenBlock)) {
            throw new IllegalArgumentException("Instance type must be an instance of OraxenBlock");
        }

        final Location location = instance.getLocation();
        final Block block = location.getBlock();

        // Try block-class kinds first (note + string blocks live as real BlockData).
        if (OraxenBlocks.isOraxenBlock(block)) {
            return OraxenBlocks.remove(location, player);
        }

        // Furniture lives off the location grid as ItemDisplay entities; use the furniture
        // remove API which handles base-entity cleanup.
        if (OraxenFurniture.getFurnitureMechanic(block) != null) {
            return OraxenFurniture.remove(location, player);
        }
        return false;
    }

    @Override
    public @NotNull SmartBlockBreakOverride getBreakOverrideDefaults(@NotNull SmartBlockInstance instance,
                                                                      @NotNull Player player,
                                                                      @NotNull ItemStack held) {
        if (!(instance.getType() instanceof OraxenBlock oraxenBlock)) {
            return SmartBlockBreakOverride.empty();
        }

        // Oraxen's hardness is a vanilla-style integer (wood=2, stone=4, …) — passthrough
        // to a double on the same scale. No multipliers exposed at this layer, so the
        // override only carries hardness; speed/tool resolution stays with vanilla / globals.
        final OptionalInt hardness = lookupHardness(oraxenBlock.getId());
        if (hardness.isEmpty()) return SmartBlockBreakOverride.empty();

        return SmartBlockBreakOverride.builder()
                .hardness(hardness.getAsInt())
                .build();
    }

    /** Resolves a hardness value across Oraxen's three mechanic kinds. Returns empty if none configures it. */
    private static OptionalInt lookupHardness(@NotNull String id) {
        final NoteBlockMechanic note = OraxenBlocks.getNoteBlockMechanic(id);
        if (note != null && note.hasHardness()) return OptionalInt.of(note.getHardness());

        final StringBlockMechanic string = OraxenBlocks.getStringMechanic(id);
        if (string != null && string.hasHardness()) return OptionalInt.of(string.getHardness());

        final FurnitureMechanic furniture = OraxenFurniture.getFurnitureMechanic(id);
        if (furniture != null && furniture.hasHardness()) return OptionalInt.of(furniture.getHardness());

        return OptionalInt.empty();
    }
}
