package me.mykindos.betterpvp.core.metal;

import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a block of metal in the game.
 */
public abstract class MetalBlock extends SmartBlock implements NexoBlock {

    private final String nexoId;

    protected MetalBlock(String id, String name, String nexoId) {
        super(id, name);
        this.nexoId = nexoId;
    }

    @Override
    public @NotNull String getId() {
        return nexoId;
    }
}
