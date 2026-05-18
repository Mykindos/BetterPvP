package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.Mechanic;
import com.nexomc.nexo.mechanics.breakable.Breakable;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
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
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory class for creating {@link SmartBlockInstance}s
 */
@Singleton
public class NexoSmartBlockFactory implements SmartBlockFactory {

    private final SmartBlockRegistry smartBlockRegistry;
    private final SmartBlockDataManager dataManager;
    private final ClientManager clientManager;

    /** Reverse index Nexo-id → SmartBlock, rebuilt only when the registry grows. */
    private final Map<String, SmartBlock> nexoIdIndex = new HashMap<>();
    private int indexedRegistrySize = -1;

    @Inject
    private NexoSmartBlockFactory(SmartBlockRegistry smartBlockRegistry,
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
                if (!NexoFurniture.isFurniture(display)) {
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
            final Vector3f translation = new Vector3f(furnitureMechanic.getProperties().getTranslation());
            translation.rotateX((float) Math.toRadians(location.getPitch()));
            translation.rotateY((float) Math.toRadians(-location.getYaw()));
            location.add(translation.x(), translation.y(), translation.z());
        }

        return new SmartBlockInstance(type, location, dataManager);
    }

    public Optional<Mechanic> mechanic(Block block) {
        return Optional.ofNullable(((Mechanic) NexoBlocks.chorusBlockMechanic(block.getBlockData())))
                .or(() -> Optional.ofNullable(NexoBlocks.customBlockMechanic(block.getBlockData())))
                .or(() -> Optional.ofNullable(NexoBlocks.noteBlockMechanic(block.getBlockData())))
                .or(() -> Optional.ofNullable(NexoBlocks.stringMechanic(block.getBlockData())))
                .or(() -> Optional.ofNullable(NexoFurniture.furnitureMechanic(block)));
    }

    private SmartBlock getBlock(@NotNull String nexoId) {
        final Map<String, SmartBlock> all = smartBlockRegistry.getAllBlocks();
        if (all.size() != indexedRegistrySize) {
            nexoIdIndex.clear();
            for (SmartBlock block : all.values()) {
                if (block instanceof NexoBlock nexoBlock) {
                    nexoIdIndex.putIfAbsent(nexoBlock.getId(), block);
                }
            }
            indexedRegistrySize = all.size();
        }
        return nexoIdIndex.get(nexoId);
    }

    public Optional<Mechanic> mechanic(Entity entity) {
        return Optional.ofNullable(NexoFurniture.furnitureMechanic(entity));
    }

    public Optional<SmartBlockInstance> from(Location location) {
        final ItemDisplay itemDisplay = FurnitureMechanic.Companion.baseEntity(location);
        return itemDisplay == null ? Optional.empty() : from(itemDisplay);
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
            if (block != null && (NexoBlocks.isNexoChorusBlock(block) ||
                    NexoBlocks.isCustomBlock(block) ||
                    NexoBlocks.isNexoNoteBlock(block) ||
                    NexoBlocks.isNexoStringBlock(block))) {
                return from(block); // Should not hit Furniture predicate, therefore thread safe
            }
        }

        final ItemDisplay targetFurniture = NexoFurniture.findTargetFurniture(player);
        if (targetFurniture == null) {
            return Optional.empty();
        }

        return mechanic(targetFurniture).flatMap(mechanic -> {
            final SmartBlock smartBlock = getBlock(mechanic.getItemID());
            return Optional.of(create(smartBlock, targetFurniture.getLocation(), mechanic));
        });
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

        return NexoBlocks.chorusBlockMechanic(block) != null ||
                NexoBlocks.customBlockMechanic(block) != null ||
                NexoBlocks.noteBlockMechanic(block) != null ||
                NexoBlocks.stringMechanic(block) != null ||
                NexoFurniture.furnitureMechanic(block) != null;
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
    public boolean isTargetSmartBlock(@NotNull Player player) {
        final AttributeInstance attribute = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE));
        final double blockRange = attribute.getValue();
        final RayTraceResult rayTraceResult = player.rayTraceBlocks(blockRange);
        if (rayTraceResult != null) {
            final Block block = rayTraceResult.getHitBlock();
            if (block != null && (NexoBlocks.isNexoChorusBlock(block) ||
                    NexoBlocks.isCustomBlock(block) ||
                    NexoBlocks.isNexoNoteBlock(block) ||
                    NexoBlocks.isNexoStringBlock(block))) {
                return true;
            }
        }

        return NexoFurniture.findTargetFurniture(player) != null;
    }

    @Override
    public boolean isSmartBlock(Location location) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This method must be called on the main thread");
        }
        return from(location).isPresent();
    }

    @Override
    public BlockData createBlockData(SmartBlock type) {
        if (!(type instanceof NexoBlock block)) {
            throw new IllegalArgumentException("Type must be an instance of NexoBlock");
        }
        return NexoBlocks.blockData(block.getId());
    }

    @Override
    public @NotNull SmartBlockBreakOverride getBreakOverrideDefaults(@NotNull SmartBlockInstance instance,
                                                                      @NotNull Player player,
                                                                      @NotNull org.bukkit.inventory.ItemStack held) {
        if (!(instance.getType() instanceof NexoBlock nexoBlock)) {
            return SmartBlockBreakOverride.empty();
        }

        final Breakable breakable = lookupBreakable(nexoBlock.getId());
        if (breakable == null) return SmartBlockBreakOverride.empty();

        // Nexo's hardness is vanilla-scaled (verified: Nexo anvil 5.0 == vanilla anvil 5.0).
        // The multiplier methods take the player so Nexo can resolve "is the right tool held"
        // internally — we consume the resolved values and leave requiredTool empty in the
        // override (multiplier always applies from our side).
        final SmartBlockBreakOverride.Builder builder = SmartBlockBreakOverride.builder();
        builder.hardness(breakable.getHardness()); // magic number

        final double toolSpeedMultiplier = breakable.toolSpeedMultiplier(player);
        if (toolSpeedMultiplier > 0) {
            builder.toolSpeedMultiplier(toolSpeedMultiplier / 4); // magic number
        }

        final double speedMultiplier = breakable.speedMultiplier(player);
        if (speedMultiplier > 0) {
            builder.speedMultiplier(speedMultiplier);
        }
        return builder.build();
    }

    /**
     * Resolves Nexo's {@code Breakable} from a block id across all four block-class
     * mechanic kinds plus furniture. Returns {@code null} if no mechanic is registered.
     */
    private static Breakable lookupBreakable(@NotNull String id) {
        CustomBlockMechanic cb = NexoBlocks.customBlockMechanic(id);
        if (cb != null) return cb.getBreakable();
        cb = NexoBlocks.noteBlockMechanic(id);
        if (cb != null) return cb.getBreakable();
        cb = NexoBlocks.stringMechanic(id);
        if (cb != null) return cb.getBreakable();
        cb = NexoBlocks.chorusBlockMechanic(id);
        if (cb != null) return cb.getBreakable();
        final FurnitureMechanic fm = NexoFurniture.furnitureMechanic(id);
        return fm == null ? null : fm.getBreakable();
    }

    @Override
    public void displayBreakProgress(@NotNull Player player, @NotNull Block block, double progress) {
        if (!NexoFurniture.isFurniture(block.getLocation())) return; // only display for furniture
        final float clamped = (float) Math.max(0.0, Math.min(1.0, progress));
        final TextComponent bar = ProgressBar.withProgress(clamped)
                .withProgressColor(NamedTextColor.WHITE)
                .withRemainingColor(NamedTextColor.DARK_GRAY)
                .withCharacter(' ')
                .build()
                .decorate(TextDecoration.STRIKETHROUGH);
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(500, TitleComponent.subtitle(0.0,
                0.2,
                0.0,
                false,
                g -> bar));
    }

    @Override
    public boolean breakBlock(Player player, SmartBlockInstance instance) {
        if (!(instance.getType() instanceof NexoBlock)) {
            throw new IllegalArgumentException("Instance type must be an instance of NexoBlock");
        }

        final Location location = instance.getLocation();
        final CustomBlockMechanic customBlockMechanic = NexoBlocks.customBlockMechanic(location.getBlock());
        if (customBlockMechanic != null) {
            if (customBlockMechanic.hasBlockSounds() && Objects.requireNonNull(customBlockMechanic.getBlockSounds()).hasBreakSound()) {
                final String sound = Objects.requireNonNull(customBlockMechanic.getBlockSounds().getBreakSound());
                final float pitch = customBlockMechanic.getBlockSounds().getBreakPitch();
                final float volume = customBlockMechanic.getBlockSounds().getBreakVolume();
                player.playSound(location, sound, pitch, volume);
            }
            return NexoBlocks.remove(location, player);
        }

        final FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(location.getBlock());
        if (mechanic != null) {
            if (mechanic.getHasBlockSounds() && Objects.requireNonNull(mechanic.getBlockSounds()).hasBreakSound()) {
                final String sound = Objects.requireNonNull(mechanic.getBlockSounds().getBreakSound());
                final float pitch = mechanic.getBlockSounds().getBreakPitch();
                final float volume = mechanic.getBlockSounds().getBreakVolume();
                player.playSound(location, sound, pitch, volume);
            }
            return NexoFurniture.remove(location, player);
        }
        return false;
    }
}
