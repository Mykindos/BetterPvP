package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ModelEngineHelper {

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
