package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

@UtilityClass
public class ModelEngineHelper {

    /**
     * Resolves or creates the {@link ModeledEntity} wrapper for a backing entity and disables
     * culling so the model keeps animating regardless of camera angle. Supports both construction
     * flows: a vanilla entity that has no wrapper yet (one is created now), and a ModelEngine
     * dummy entity that already has a wrapper (only cull settings are applied).
     * <p>
     * Used by {@code ModeledNPC}, {@code ModeledProp}, and {@code SceneMob}.
     *
     * @param entity       the backing entity to wrap
     * @param initConsumer optional consumer applied when a new wrapper is created (ignored if one exists)
     * @return the bound modeled entity with culling disabled
     */
    @NotNull
    public static ModeledEntity bind(@NotNull Entity entity, @Nullable Consumer<ModeledEntity> initConsumer) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null) {
            modeledEntity = ModelEngineAPI.createModeledEntity(entity, initConsumer);
        }
        modeledEntity.getBase().getData().setBackCullType(CullType.NO_CULL);
        modeledEntity.getBase().getData().setBlockedCullType(CullType.NO_CULL);
        modeledEntity.getBase().getData().setVerticalCullType(CullType.NO_CULL);
        return modeledEntity;
    }

    /** @see #bind(Entity, Consumer) */
    @NotNull
    public static ModeledEntity bind(@NotNull Entity entity) {
        return bind(entity, null);
    }

    /**
     * Remaps every bone in the given model to the corresponding bone in the given blueprint.
     * If a bone in the blueprint is not found in the model, it is ignored.
     * <p>
     * Very useful for applying skins to models in "Scenes" packs.
     * @param model the model to remap
     * @param modelBlueprint the blueprint to remap to
     */
    public static void remapModel(ActiveModel model, ModelBlueprint modelBlueprint) {
        final ModelBlueprint out = model.getBlueprint();
        final Map<String, BlueprintBone> inFlatMap = modelBlueprint.getFlatMap();
        final Map<String, BlueprintBone> outFlatMap = out.getFlatMap();

        for (String bone : new HashSet<>(outFlatMap.keySet())) {
            if (inFlatMap.containsKey(bone)) {
                final ModelBone modelBone = model.getBone(bone).orElseThrow();
                if (modelBone.isRenderer()) {
                    modelBone.setModel(inFlatMap.get(bone));
                }
            }
        }
    }

    public static boolean isExclusivelyPlayingAnimation(ActiveModel model, String animationId) {
        final AnimationHandler animationHandler = model.getAnimationHandler();
        Preconditions.checkNotNull(animationHandler, "Animation handler cannot be null");
        final Map<String, IAnimationProperty> animations = animationHandler.getAnimations();
        return animations.size() == 1 && animations.containsKey(animationId);
    }

    public static void playAnimation(ActiveModel model, String animationId) {
        playAnimation(model, animationId, 1);
    }

    public static void playAnimation(ActiveModel model, String animationId, double speed) {
        playAnimation(model, animationId, speed, true);
    }

    /**
     * @param force {@code true} restarts/overrides the clip even if it is already playing;
     *              {@code false} lets ModelEngine dedupe (a no-op if this clip is already playing),
     *              which is what makes re-requesting a looping clip every tick cheap.
     */
    public static void playAnimation(ActiveModel model, String animationId, double speed, boolean force) {
        playAnimation(model, animationId, 0, 0, speed, force);
    }

    /**
     * Plays a clip with explicit blend times so it crossfades rather than snapping.
     *
     * @param lerpIn  seconds spent interpolating <i>into</i> the clip (smooth fade-in)
     * @param lerpOut seconds spent interpolating <i>out of</i> the clip when it is later stopped
     * @param force   {@code true} restarts/overrides even if already playing; {@code false} lets
     *                ModelEngine dedupe an already-playing clip (cheap to re-request every tick).
     */
    public static void playAnimation(ActiveModel model, String animationId, double lerpIn, double lerpOut, double speed, boolean force) {
        final AnimationHandler animationHandler = model.getAnimationHandler();
        Preconditions.checkNotNull(animationHandler, "Animation handler cannot be null");
        animationHandler.playAnimation(animationId, lerpIn, lerpOut, speed, force);
    }

    /**
     * Gracefully stops a clip, playing its lerp-out so it fades rather than popping off. No-op if the
     * clip is not currently playing. Use this when swapping looping clips so the outgoing and incoming
     * clips crossfade instead of stacking.
     */
    public static void stopAnimation(ActiveModel model, String animationId) {
        final AnimationHandler animationHandler = model.getAnimationHandler();
        Preconditions.checkNotNull(animationHandler, "Animation handler cannot be null");
        animationHandler.stopAnimation(animationId);
    }

    public static void randomAnimation(ActiveModel model, String... animationIds) {
        Preconditions.checkNotNull(animationIds, "Animation IDs cannot be null");
        Preconditions.checkArgument(animationIds.length > 0, "At least one animation ID must be provided");
        String random = animationIds[UtilMath.randomInt(0, animationIds.length)];
        playAnimation(model, random);
    }

}
