package me.mykindos.betterpvp.core.scene.npc;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.scene.HasModeledEntity;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * An NPC backed by a ModelEngine {@link ModeledEntity}.
 * <p>
 * The ModelEngine entity is created/configured inside {@link #onInit()} after the
 * backing entity is bound, supporting two construction flows:
 * <p>
 * <b>Standard flow</b> (vanilla entity created first):
 * <pre>
 *   IronGolem golem = world.spawn(..., IronGolem.class);
 *   ModeledNPC npc = new ModeledNPC(factory, consumer);
 *   npc.init(golem);  // onInit() wraps golem into a ModeledEntity
 * </pre>
 *
 * <b>ME dummy-entity flow</b> (ModelEngine creates the base entity externally):
 * <pre>
 *   ModeledEntity me = ModelEngineAPI.createModeledEntity(...);
 *   Entity dummyEntity = me.getBase().getBukkitEntity();
 *   ModeledNPC npc = new ModeledNPC(factory, null);
 *   npc.init(dummyEntity);  // onInit() finds existing ModeledEntity, applies cull settings only
 * </pre>
 */
public class ModeledNPC extends NPC implements HasModeledEntity {

    @Nullable private Consumer<ModeledEntity> initConsumer;

    public ModeledNPC(SceneObjectFactory factory, @Nullable Consumer<ModeledEntity> initConsumer) {
        super(factory);
        this.initConsumer = initConsumer;
    }

    public ModeledNPC(SceneObjectFactory factory) {
        this(factory, (Consumer<ModeledEntity>) null);
    }

    /**
     * @deprecated Use {@link #ModeledNPC(SceneObjectFactory, Consumer)} and call
     *             {@link #init(org.bukkit.entity.Entity)} separately.
     *             Retained for backward compatibility while existing callers are migrated.
     */
    @Deprecated
    public ModeledNPC(SceneObjectFactory factory, org.bukkit.entity.Entity entity, @Nullable Consumer<ModeledEntity> consumer) {
        this(factory, consumer);
        init(entity);
    }

    /** @deprecated See {@link #ModeledNPC(SceneObjectFactory, org.bukkit.entity.Entity, Consumer)}. */
    @Deprecated
    public ModeledNPC(SceneObjectFactory factory, org.bukkit.entity.Entity entity) {
        this(factory, entity, null);
    }

    @Override
    protected void onInit() {
        ModelEngineHelper.bind(getEntity(), initConsumer);
        this.initConsumer = null; // release - only needed once
    }

    @Override
    @Nullable
    public ModeledEntity getModeledEntity() {
        if (!isInitialized()) {
            return null;
        }
        return ModelEngineAPI.getModeledEntity(getEntity());
    }

    @Override
    protected void onDematerialize() {
        final ModeledEntity modeledEntity = getModeledEntity();
        if (modeledEntity != null) {
            modeledEntity.markRemoved();
        }
        super.onDematerialize();
    }

}
