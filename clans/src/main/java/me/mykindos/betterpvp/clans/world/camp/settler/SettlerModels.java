package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.TagBehavior;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dresses a camp NPC that looks like a settler without being one on the roster, such as the Steward or a candidate
 * waiting at the Dock: the look's model, or the default one while it is not installed, and a nameplate.
 */
@Singleton
public class SettlerModels {

    private final SettlerConfig config;

    @Inject
    public SettlerModels(@NotNull SettlerConfig config) {
        this.config = config;
    }

    /** Puts {@code look} on {@code npc} with a nameplate. Call from a decorator, so it is redone each time it spawns. */
    public void dress(@NotNull ModeledNPC npc, @NotNull SettlerLook look, @NotNull Component name,
                      @NotNull Component role) {
        final ActiveModel model = model(npc, look);
        if (model != null) {
            BoneTagAnchor.addNameplate(npc, model, "head", name, role);
        } else {
            TagBehavior.addNameplate(npc, name, role);
        }
    }

    private @Nullable ActiveModel model(@NotNull ModeledNPC npc, @NotNull SettlerLook wanted) {
        final ModeledEntity modeled = npc.getModeledEntity();
        if (modeled == null) {
            return null;
        }
        final boolean installed = ModelEngineAPI.getBlueprint(wanted.getModel()) != null
                && (wanted.getSkin() == null || ModelEngineAPI.getBlueprint(wanted.getSkin()) != null);
        final SettlerLook look = installed ? wanted : config.defaultLook();
        if (ModelEngineAPI.getBlueprint(look.getModel()) == null) {
            return null;
        }
        final ActiveModel model = ModelEngineAPI.createActiveModel(look.getModel());
        model.setScale(look.getSize());
        model.setHitboxScale(1.5);
        model.getAnimationHandler().setDefaultProperty(
                new AnimationHandler.DefaultProperty(ModelState.IDLE, look.getIdleAnimation(), 0, 0, 1));
        modeled.addModel(model, true);
        if (look.getSkin() != null && ModelEngineAPI.getBlueprint(look.getSkin()) != null) {
            ModelEngineHelper.remapModel(model, ModelEngineAPI.getBlueprint(look.getSkin()));
        }
        return model;
    }
}
