package me.mykindos.betterpvp.core.interaction;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.condition.ConditionResult;
import me.mykindos.betterpvp.core.interaction.condition.InteractionCondition;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation of {@link Interaction} providing common functionality.
 */
@Getter
public abstract class AbstractInteraction implements Interaction {

    protected final String name;
    protected final String description;
    protected final List<InteractionCondition> conditions;

    protected boolean consumesItem = false;

    protected AbstractInteraction(@NotNull String name, @NotNull String description) {
        Preconditions.checkArgument(!name.isEmpty(), "Name cannot be empty");
        Preconditions.checkArgument(!description.isEmpty(), "Description cannot be empty");
        this.name = name;
        this.description = description;
        this.conditions = new ArrayList<>();
    }

    protected AbstractInteraction(@NotNull String name, @NotNull String description,
                                  @NotNull List<InteractionCondition> conditions) {
        Preconditions.checkArgument(!name.isEmpty(), "Name cannot be empty");
        Preconditions.checkArgument(!description.isEmpty(), "Description cannot be empty");
        this.name = name;
        this.description = description;
        this.conditions = new ArrayList<>(conditions);
    }

    @Override
    public @NotNull List<InteractionCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    @Override
    public @NotNull InteractionResult execute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                               @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // Check all conditions
        for (InteractionCondition condition : conditions) {
            ConditionResult result = condition.check(actor, context);
            if (!result.passed()) {
                return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS, result.failureMessage());
            }
        }

        // Delegate to subclass implementation
        return doExecute(actor, context, itemInstance, itemStack);
    }

    /**
     * Perform the actual interaction execution.
     * Called after all conditions have been checked.
     *
     * @param actor        the actor performing the interaction
     * @param context      the interaction context
     * @param itemInstance the item instance (may be null)
     * @param itemStack    the item stack (may be null)
     * @return the result of the interaction
     */
    @NotNull
    protected abstract InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack);

    /**
     * Add a condition to this interaction.
     *
     * @param condition the condition to add
     */
    protected void addCondition(@NotNull InteractionCondition condition) {
        conditions.add(condition);
    }

    /**
     * Set whether this interaction consumes the item.
     *
     * @param consumesItem true if the item should be consumed
     */
    public void setConsumesItem(boolean consumesItem) {
        this.consumesItem = consumesItem;
    }

    @Override
    public boolean consumesItem() {
        return consumesItem;
    }
}
