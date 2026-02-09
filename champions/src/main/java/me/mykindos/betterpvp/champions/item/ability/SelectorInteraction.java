package me.mykindos.betterpvp.champions.item.ability;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.inventory.util.TriConsumer;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntitySelector;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.EntityOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.ShapeEntitySelector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Getter
@Setter
public class SelectorInteraction extends AbstractInteraction {

    private @NotNull Function<LivingEntity, EntitySelector<LivingEntity>> selector = entity ->
            ShapeEntitySelector.box(new EntityOrigin(entity, true), 2.0, 2.5, 1.0);

    private final TriConsumer<InteractionActor, InteractionContext, LivingEntity> entityConsumer;

    public SelectorInteraction(TriConsumer<InteractionActor, InteractionContext, LivingEntity> entityConsumer) {
        super("selector");
        this.entityConsumer = entityConsumer;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        final LivingEntity entity = actor.getEntity();
        // select all the entities and attack them
        selector.apply(entity)
                .select()
                .forEach(target -> onSelect(actor, context, target));
        return InteractionResult.Success.ADVANCE;
    }

    protected void onSelect(@NotNull InteractionActor actor, @NotNull InteractionContext context, LivingEntity livingEntity) {
        entityConsumer.accept(actor, context, livingEntity);
    }
}
