package me.mykindos.betterpvp.core.interaction;

import me.mykindos.betterpvp.core.interaction.condition.InteractionCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A placeholder interaction that only consumes the execution. These interactions are not
 * displayed in the interaction menu.
 */
public abstract class DummyInteraction extends AbstractInteraction {

    private final InteractionCondition[] conditions;

    protected DummyInteraction(InteractionCondition... conditions) {
        super("_dummy");
        this.conditions = conditions;
    }

    @Override
    public @NotNull String getName() {
        return "_dummy";
    }

    @Override
    public @NotNull List<InteractionCondition> getConditions() {
        return List.of(conditions);
    }

}
