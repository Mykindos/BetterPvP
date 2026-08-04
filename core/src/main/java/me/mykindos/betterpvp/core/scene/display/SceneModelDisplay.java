package me.mykindos.betterpvp.core.scene.display;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import me.mykindos.betterpvp.core.scene.HasModeledEntity;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link SceneObject} that is nothing but a ModelEngine model — no behaviours, no interaction, no factory.
 * <p>
 * {@link me.mykindos.betterpvp.core.scene.prop.ModeledProp} covers a modeled thing that lives in the world and is
 * spawned through a factory; this covers a modeled thing something else owns and positions, such as an indicator.
 */
public class SceneModelDisplay extends SceneObject implements HasModeledEntity {

    private final String modelId;
    private final String idleAnimation;
    private final double scale;
    @Nullable private final Display.Billboard billboard;

    /**
     * @param idleAnimation clip looped as the model's idle state; blank leaves the model on whatever its blueprint
     *                      already defaults to
     */
    public SceneModelDisplay(@NotNull String modelId, @NotNull String idleAnimation, double scale) {
        this(modelId, idleAnimation, scale, null);
    }

    /**
     * @param billboard which axes the model turns on to face whoever is looking, as {@link Display} does — but applied
     *                  to the model's own bones, since the backing entity is never what the client renders.
     *                  {@code null} leaves the blueprint's own setting alone.
     */
    public SceneModelDisplay(@NotNull String modelId, @NotNull String idleAnimation, double scale,
                             @Nullable Display.Billboard billboard) {
        this.modelId = modelId;
        this.idleAnimation = idleAnimation;
        this.scale = scale;
        this.billboard = billboard;
    }

    @Override
    protected void onInit() {
        final ModeledEntity modeled = ModelEngineHelper.bind(getEntity());

        final ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
        model.setScale(scale);
        if (!idleAnimation.isBlank()) {
            model.getAnimationHandler().setDefaultProperty(
                    new AnimationHandler.DefaultProperty(ModelState.IDLE, idleAnimation, 0, 0, 1));
        }
        modeled.addModel(model, true);
        style(model);
    }

    /**
     * Lights the model from itself rather than from the block it happens to be standing in, so it reads the same in a
     * ship's hold at night as it does on deck at noon, and turns it to face the viewer if asked.
     * <p>
     * Both are set per bone as well as on the model: a bone is its own display entity client-side, and that is the
     * level the client actually honours these at.
     */
    private void style(@NotNull ActiveModel model) {
        model.setBlockLight(15);
        model.setSkyLight(15);
        model.setShadowVisible(false);
        model.setGlowing(true);
        if (billboard != null) {
            model.setBillboard(billboard);
        }

        for (ModelBone bone : model.getBones().values()) {
            bone.setBlockLight(15);
            bone.setSkyLight(15);
            if (billboard != null) {
                bone.setBillboard(billboard);
            }
        }
    }

    @Override
    protected void onDematerialize() {
        final ModeledEntity modeled = getModeledEntity();
        if (modeled != null) {
            modeled.markRemoved();
        }
    }

    @Override
    @Nullable
    public ModeledEntity getModeledEntity() {
        if (!isInitialized()) {
            return null;
        }
        return ModelEngineAPI.getModeledEntity(getEntity());
    }
}
