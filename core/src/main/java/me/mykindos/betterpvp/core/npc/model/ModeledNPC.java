package me.mykindos.betterpvp.core.npc.model;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModeledNPC extends NPC {

    public ModeledNPC(NPCFactory factory, Entity entity, Consumer<ModeledEntity> consumer) {
        super(entity, factory);
        ModelEngineAPI.createModeledEntity(entity, consumer);
    }

    public ModeledNPC(NPCFactory factory, Entity entity) {
        this(factory, entity, null);
    }

    @NotNull
    public ModeledEntity getModeledEntity() {
        return ModelEngineAPI.getModeledEntity(entity);
    }

    @Override
    public void remove() {
        final ModeledEntity modeledEntity = getModeledEntity();
        modeledEntity.markRemoved();
        super.remove();
    }
}
