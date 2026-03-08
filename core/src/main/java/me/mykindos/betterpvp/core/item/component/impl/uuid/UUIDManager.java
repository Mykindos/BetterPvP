package me.mykindos.betterpvp.core.item.component.impl.uuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;

@Singleton
public class UUIDManager extends Manager<String, UUIDItem> {
    @Getter
    private final UUIDRepository uuidRepository;

    @Inject
    public UUIDManager(UUIDRepository uuidRepository) {
        this.uuidRepository = uuidRepository;
    }

    public void loadObjectsFromNamespace(String namespace) {
        loadFromList(uuidRepository.getUUIDItemsForModule(namespace));
    }

    public void loadFromList(List<UUIDItem> objects) {
        objects.forEach(uuidItem -> addObject(uuidItem.getUuid().toString(), uuidItem));
    }

    public void addUuid(UUIDItem object) {
        if (getObject(object.getUuid().toString()).isPresent()){
            return;
        }
        addObject(object.getUuid().toString(), object);
        uuidRepository.save(object);
    }
}