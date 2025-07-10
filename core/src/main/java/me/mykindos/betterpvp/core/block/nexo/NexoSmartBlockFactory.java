package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.Mechanic;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager;
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox;
import com.nexomc.nexo.mechanics.furniture.hitbox.FurnitureHitbox;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory class for creating {@link SmartBlockInstance}s
 */
@Singleton
public class NexoSmartBlockFactory implements SmartBlockFactory {

    private final SmartBlockRegistry smartBlockRegistry;
    private final SmartBlockDataManager dataManager;

    @Inject
    private NexoSmartBlockFactory(SmartBlockRegistry smartBlockRegistry, SmartBlockDataManager dataManager) {
        this.smartBlockRegistry = smartBlockRegistry;
        this.dataManager = dataManager;
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
        return Optional.ofNullable(((Mechanic) NexoBlocks.chorusBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.customBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.noteBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.stringMechanic(block)))
                .or(() -> Optional.ofNullable(NexoFurniture.furnitureMechanic(block)));
    }

    // todo: make this O(1)
    private SmartBlock getBlock(@NotNull String nexoId) {
        return smartBlockRegistry.getAllBlocks().values().stream()
                .filter(block -> block instanceof NexoBlock)
                .filter(nexoBlock -> ((NexoBlock) nexoBlock).getId().equals(nexoId))
                .findFirst()
                .orElse(null);
    }

    public Optional<Mechanic> mechanic(Entity entity) {
        return Optional.ofNullable(NexoFurniture.furnitureMechanic(entity));
    }

    @Override
    public Optional<SmartBlockInstance> from(Block block) {
        return mechanic(block).map(mechanic -> {
            final SmartBlock smartBlock = getBlock(mechanic.getItemID());
            return create(smartBlock, block.getLocation(), mechanic);
        });
    }

    public Optional<SmartBlockInstance> from(Entity entity) {
        return mechanic(entity).map(mechanic -> {
            final SmartBlock smartBlock = getBlock(mechanic.getItemID());
            return create(smartBlock, entity.getLocation(), mechanic);
        });
    }

    @Override
    public boolean isSmartBlock(Block block) {
        return NexoBlocks.chorusBlockMechanic(block) != null ||
                NexoBlocks.customBlockMechanic(block) != null ||
                NexoBlocks.noteBlockMechanic(block) != null ||
                NexoBlocks.stringMechanic(block) != null ||
                NexoFurniture.furnitureMechanic(block) != null;
    }
}
