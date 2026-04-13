package me.mykindos.betterpvp.core.scene.prop;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.scene.HasModeledEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Prop} backed by a ModelEngine {@link ModeledEntity}.
 * <p>
 * Mirrors the ModelEngine setup in {@link me.mykindos.betterpvp.core.scene.npc.ModeledNPC}
 * but without behaviors or interaction: subclasses just add models in their {@link #onInit()}
 * after calling {@code super.onInit()}.
 * <p>
 * Example subclass:
 * <pre>
 *   public class MarketStallProp extends ModeledProp {
 *       public MarketStallProp(PropFactory factory) { super(factory); }
 *
 *       {@literal @}Override
 *       protected void onInit() {
 *           super.onInit(); // wraps entity in ModelEngine
 *           ActiveModel model = ModelEngineAPI.createActiveModel("market_stall");
 *           getModeledEntity().addModel(model, true);
 *       }
 *   }
 * </pre>
 */
public abstract class ModeledProp extends Prop implements HasModeledEntity {

    protected ModeledProp(PropFactory factory) {
        super(factory);
    }

    @Override
    protected void onInit() {
        ModeledEntity me = ModelEngineAPI.getModeledEntity(getEntity());
        if (me == null) {
            me = ModelEngineAPI.createModeledEntity(getEntity(), null);
        }
        me.getBase().getData().setBackCullType(CullType.NO_CULL);
        me.getBase().getData().setBlockedCullType(CullType.NO_CULL);
        me.getBase().getData().setVerticalCullType(CullType.NO_CULL);
    }

    @Nullable
    public ModeledEntity getModeledEntity() {
        if (!isInitialized()) {
            return null;
        }
        return ModelEngineAPI.getModeledEntity(getEntity());
    }

    @Override
    public void remove() {
        final ModeledEntity me = getModeledEntity();
        if (me != null) {
            me.markRemoved();
        }
        super.remove();
    }
}
