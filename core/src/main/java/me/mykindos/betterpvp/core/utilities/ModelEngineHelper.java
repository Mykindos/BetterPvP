package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Map;

@UtilityClass
public class ModelEngineHelper {

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
        final AnimationHandler animationHandler = model.getAnimationHandler();
        Preconditions.checkNotNull(animationHandler, "Animation handler cannot be null");
        animationHandler.playAnimation(animationId, 0, 0, speed, true);
    }

    public static void randomAnimation(ActiveModel model, String... animationIds) {
        Preconditions.checkNotNull(animationIds, "Animation IDs cannot be null");
        Preconditions.checkArgument(animationIds.length > 0, "At least one animation ID must be provided");
        String random = animationIds[UtilMath.randomInt(0, animationIds.length)];
        playAnimation(model, random);
    }

}
