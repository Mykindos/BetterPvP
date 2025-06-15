package me.mykindos.betterpvp.core.block;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.block.behavior.ClickBehavior;
import me.mykindos.betterpvp.core.block.behavior.StorageBehavior;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a custom block type with specific behaviors and properties.
 */
public abstract class SmartBlock {

    private final String id;
    private final String name;
    private ClickBehavior clickBehavior;
    private StorageBehavior storageBehavior;

    protected SmartBlock(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
    }

    protected void setClickBehavior(@NotNull ClickBehavior clickBehavior) {
        Preconditions.checkState(this.clickBehavior == null, "Click behavior is already set for this block: " + id);
        this.clickBehavior = clickBehavior;
    }

    public Optional<ClickBehavior> getClickBehavior() {
        return Optional.ofNullable(clickBehavior);
    }

    protected void setStorageBehavior(@NotNull StorageBehavior storageBehavior) {
        Preconditions.checkState(this.storageBehavior == null, "Storage behavior is already set for this block: " + id);
        this.storageBehavior = storageBehavior;
    }

    public Optional<StorageBehavior> getStorageBehavior() {
        return Optional.ofNullable(storageBehavior);
    }

    /**
     * @return the name of the block
     */
    public String getName() {
        return name;
    }

    /**
     * @return the namespaced key of the block
     */
    public String getKey() {
        return id;
    }

}
