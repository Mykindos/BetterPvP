package me.mykindos.betterpvp.core.block;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.bukkit.block.Block;

/**
 * Represents an instance of a {@link SmartBlock} in the game.
 */
@Getter
@EqualsAndHashCode
public final class SmartBlockInstance {

    private final SmartBlock smartBlock;
    @Delegate
    private final Block handle;

    public SmartBlockInstance(SmartBlock smartBlock, Block handle) {
        this.smartBlock = smartBlock;
        this.handle = handle;
    }
}
