package me.mykindos.betterpvp.core.items.uuiditem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;

@Singleton
public class UUIDManager extends Manager<UUIDItem> {
    UUIDRepository uuidRepository;

    @Inject
    public UUIDManager(UUIDRepository uuidRepository) {
        this.uuidRepository = uuidRepository;
    }

    public void loadObjectsFromNamespace(String namespace) {
        loadFromList(uuidRepository.getUUIDItemsForModule(namespace));
    }

    @Override
    public void loadFromList(List<UUIDItem> objects) {
        objects.forEach(uuidItem -> addObject(uuidItem.getUuid(), uuidItem));
    }

    public void addUuid(UUIDItem object) {
        if (getObject(object.getUuid()).isPresent()){
            return;
        }
        addObject(object.getUuid(), object);
        uuidRepository.save(object);
    }
}
