package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.Mechanic;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

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

    private SmartBlockInstance create(SmartBlock type, Block handle) {
        return new SmartBlockInstance(type, handle, dataManager);
    }

    public Optional<SmartBlock> mechanic(Block block) {
        return Optional.ofNullable(((Mechanic) NexoBlocks.chorusBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.customBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.noteBlockMechanic(block)))
                .or(() -> Optional.ofNullable(NexoBlocks.stringMechanic(block)))
                .or(() -> Optional.ofNullable(NexoFurniture.furnitureMechanic(block)))
                .map(Mechanic::getItemID)
                .map(this::getBlock);
    }

    // todo: make this O(1)
    private SmartBlock getBlock(@NotNull String nexoId) {
        return smartBlockRegistry.getAllBlocks().values().stream()
                .filter(block -> block instanceof NexoBlock)
                .filter(nexoBlock -> ((NexoBlock) nexoBlock).getId().equals(nexoId))
                .findFirst()
                .orElse(null);
    }

    public Optional<SmartBlock> mechanic(Entity entity) {
        return Optional.ofNullable((Mechanic) NexoFurniture.furnitureMechanic(entity))
                .map(Mechanic::getItemID)
                .map(this::getBlock);
    }

    @Override
    public Optional<SmartBlockInstance> from(Block block) {
        return mechanic(block).map(smartBlock -> create(smartBlock, block));
    }

    public Optional<SmartBlockInstance> from(Entity entity) {
        return mechanic(entity).map(smartBlock -> create(smartBlock, entity.getLocation().getBlock()));
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
