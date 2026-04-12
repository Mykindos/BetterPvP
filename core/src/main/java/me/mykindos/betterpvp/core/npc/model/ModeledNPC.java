package me.mykindos.betterpvp.core.npc.model;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

public class ModeledNPC extends NPC {

    public ModeledNPC(NPCFactory factory, Entity entity, Consumer<ModeledEntity> consumer) {
        super(entity, factory);
        final ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity, consumer);
        modeledEntity.getBase().getData().setBackCullType(CullType.NO_CULL);
        modeledEntity.getBase().getData().setBlockedCullType(CullType.NO_CULL);
        modeledEntity.getBase().getData().setVerticalCullType(CullType.NO_CULL);
    }

    public ModeledNPC(NPCFactory factory, Entity entity) {
        this(factory, entity, null);
    }

    public ModeledEntity getModeledEntity() {
        return ModelEngineAPI.getModeledEntity(entity);
    }

    @Override
    public void remove() {
        final ModeledEntity modeledEntity = getModeledEntity();
        if (modeledEntity != null) modeledEntity.markRemoved();
        super.remove();
    }
}
