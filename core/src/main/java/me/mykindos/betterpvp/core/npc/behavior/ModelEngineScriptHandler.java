package me.mykindos.betterpvp.core.npc.behavior;

import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;

/**
 * Handles ModelEngine script keyframes for a single active model instance.
 * <p>
 * Script keyframes should use {@code betterpvp:<script-id>} in Blockbench.
 */
public interface ModelEngineScriptHandler {

    ActiveModel getModel();

    default boolean acceptsScript(IAnimationProperty property, String script) {
        return property.getModel() == getModel();
    }

    void onScript(IAnimationProperty property, String script);

}
