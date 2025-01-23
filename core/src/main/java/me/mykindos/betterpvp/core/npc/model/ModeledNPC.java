package me.mykindos.betterpvp.core.npc.model;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

@Getter
public class ModeledNPC extends NPC {

    protected final ModeledEntity modeledEntity;

    public ModeledNPC(NPCFactory factory, Entity entity, Consumer<ModeledEntity> consumer) {
        super(entity, factory);
        this.modeledEntity = ModelEngineAPI.createModeledEntity(entity, consumer);
    }

    public ModeledNPC(NPCFactory factory, Entity entity) {
        this(factory, entity, null);
    }

    @Override
    public void remove() {
        modeledEntity.markRemoved();
        super.remove();
    }
}
