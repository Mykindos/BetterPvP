package me.mykindos.betterpvp.clans.world.discovery;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.prop.ModeledProp;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One of the two levers flanking a ship's helm: the thing a crewman physically holds to turn the vessel.
 * <p>
 * Deliberately not an {@link me.mykindos.betterpvp.core.scene.prop.InteractiveProp}. Being an
 * {@link me.mykindos.betterpvp.core.utilities.model.Actor} makes {@code SceneInteractListener} suppress block placement
 * and item use for anyone merely <em>looking</em> at it from within interaction range, which on a deck people fight and
 * move on would be miserable. Clicks are resolved instead through {@link SteeringService}'s own entity index, exactly
 * as a cannon resolves its own.
 */
@Getter
@CustomLog
public class SteerControl extends ModeledProp {

    private static final String MODEL = "steer_control";

    /** The bone that shows which way this control is asking the ship to go. */
    private static final String ARROW_BONE = "arrow";

    private final SteerSide side;

    /** What the arrow is currently tinted, so an unchanged colour is not re-applied every tick. */
    private @Nullable Color arrowTint;

    public SteerControl(@NotNull SceneObjectFactory factory, @NotNull SteerSide side) {
        super(factory);
        this.side = side;
    }

    @Override
    protected void onInit() {
        super.onInit();
        if (!ensureModel()) {
            log.warn("Steer control could not bind a ModeledEntity for model '{}'", MODEL).submit();
        }
    }

    /**
     * Dresses the body in the model unless it is already wearing it.
     * <p>
     * Asked again on every tick rather than only at init. A control that can be clicked but not seen is worse than one
     * that is missing outright, and the bind loses to ModelEngine often enough — on the tick a freshly cloned world
     * finishes loading, with a crew still arriving in it — that it is not worth trusting to a single attempt.
     *
     * @return whether the body has a model on it now
     */
    public boolean ensureModel() {
        final ModeledEntity modeled = getModeledEntity();
        if (modeled == null) {
            return false;
        }
        if (modeled.getModel(MODEL).isPresent()) {
            return true;
        }

        final ActiveModel model = ModelEngineAPI.createActiveModel(MODEL);
        modeled.addModel(model, true);
        model.setBlockLight(15);
        model.setSkyLight(15);
        modeled.setBaseEntityVisible(false);

        // Without a radius of its own the model reaches only players already tracking the body when it appeared, which
        // on the tick a crew is still teleporting in can be nobody at all.
        modeled.getBase().setRenderRadius(512);
        modeled.setSaved(false);
        this.arrowTint = null;
        return true;
    }

    public @Nullable ActiveModel getActiveModel() {
        final ModeledEntity modeled = getModeledEntity();
        return modeled == null ? null : modeled.getModel(MODEL).orElse(null);
    }

    /** Colours the arrow. A repeat of the colour already showing is dropped rather than re-sent. */
    public void tintArrow(@NotNull Color color) {
        if (color.equals(arrowTint)) {
            return;
        }

        final ActiveModel model = getActiveModel();
        if (model == null) {
            return;
        }
        model.getBone(ARROW_BONE).filter(ModelBone::isRenderer).ifPresent(bone -> {
            bone.setDefaultTint(color);
            this.arrowTint = color;
        });
    }
}
