package me.mykindos.betterpvp.core.block.oraxen;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Factory class for creating {@link SmartBlockInstance}s using Oraxen
 */
@Singleton
public class OraxenSmartBlockFactory implements SmartBlockFactory {

    private final SmartBlockRegistry smartBlockRegistry;
    private final SmartBlockDataManager dataManager;

    @Inject
    private OraxenSmartBlockFactory(SmartBlockRegistry smartBlockRegistry, SmartBlockDataManager dataManager) {
        this.smartBlockRegistry = smartBlockRegistry;
        this.dataManager = dataManager;
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

    // todo: make this O(1)
    private SmartBlock getBlock(@NotNull String oraxenId) {
        return smartBlockRegistry.getAllBlocks().values().stream()
                .filter(OraxenBlock.class::isInstance)
                .filter(oraxenBlock -> ((OraxenBlock) oraxenBlock).getId().equals(oraxenId))
                .findFirst()
                .orElse(null);
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
        return OraxenBlocks.isOraxenBlock(block) ||
                OraxenFurniture.getFurnitureMechanic(block) != null;
    }

    @Override
    public boolean isSmartBlock(Location location) {
        return from(location).isPresent();
    }
}
