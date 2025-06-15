package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.Mechanic;
import io.lumine.mythic.bukkit.utils.pdc.DataType;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Optional;

/**
 * Factory class for creating {@link SmartBlockInstance}s
 */
@Singleton
public class NexoSmartBlockFactory implements SmartBlockFactory {

    private final SmartBlockRegistry smartBlockRegistry;

    @Inject
    private NexoSmartBlockFactory(SmartBlockRegistry smartBlockRegistry) {
        this.smartBlockRegistry = smartBlockRegistry;
    }

    private SmartBlockInstance create(SmartBlock type, Block handle) {
        return new SmartBlockInstance(type, handle);
    }

    public Optional<SmartBlock> mechanic(Block block) {
        return Optional.ofNullable(((Mechanic) NexoBlocks.chorusBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.customBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.noteBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.stringMechanic(block)))
                .or(() -> Optional.ofNullable(NexoFurniture.furnitureMechanic(block)))
                .map(Mechanic::getItemID)
                .map(smartBlockRegistry::getBlock);
    }

    public Optional<SmartBlock> mechanic(Entity entity) {
        return Optional.ofNullable((Mechanic) NexoFurniture.furnitureMechanic(entity))
                .map(Mechanic::getItemID)
                .map(smartBlockRegistry::getBlock);
    }

    @Override
    public Optional<SmartBlockInstance> from(Block block) {
        return mechanic(block).map(smartBlock -> create(smartBlock, block));
    }

    public Optional<SmartBlockInstance> from(Entity entity) {
        return mechanic(entity).map(smartBlock -> create(smartBlock, entity.getLocation().getBlock()));
    }
}
