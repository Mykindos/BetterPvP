package me.mykindos.betterpvp.champions.item.ability;

import com.google.common.base.Preconditions;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Requires ModelEngine to be in the classpath
 */
@Getter
@Setter
public class VFXInteraction extends AbstractInteraction {

    @Getter(AccessLevel.NONE)
    private @NotNull BiFunction<@NotNull InteractionActor, @NotNull InteractionContext, @NotNull Location> locationMutator =
            (actor, context) -> actor.getLocation();

    private final String modelId;
    private final long tickTimeout;
    @Getter(AccessLevel.NONE)
    private Consumer<ActiveModel> modelConsumer = model -> {};
    @Getter(AccessLevel.NONE)
    private Consumer<ModeledEntity> entityConsumer = entity -> {};

    public VFXInteraction(String modelId, long tickTimeout) {
        super("vfx");
        this.modelId = modelId;
        this.tickTimeout = tickTimeout;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        final Location location = locationMutator.apply(actor, context);

        final Dummy<?> dummy = new Dummy<>();
        dummy.setVisible(true);
        dummy.setRenderRadius(60);
        dummy.syncLocation(location);
        dummy.getBodyRotationController().setRotationDelay(0);
        dummy.getBodyRotationController().setRotationDuration(0);
        ModelEngineAPI.createModeledEntity(dummy, spawned -> {
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
            activeModel.setBlockLight(15);
            activeModel.setSkyLight(15);
            Preconditions.checkNotNull(activeModel, "ModelEngine API returned null model");
            spawned.addModel(activeModel, true);
            modelConsumer.accept(activeModel);
            entityConsumer.accept(spawned);
        });

        // Schedule it's disappearance
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Champions.class), () -> {
             dummy.setRemoved(true);
        }, tickTimeout);

        return InteractionResult.Success.ADVANCE;
    }
}
