package me.mykindos.betterpvp.core.item.component.impl.uuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;

import java.util.Objects;

@Singleton
@BPvPListener
public class UUIDBuilder implements Listener {

    @Inject
    private UUIDBuilder(UUIDManager uuidManager, ItemFactory itemFactory, ItemRegistry itemRegistry) {
        itemFactory.registerDefaultBuilder(instance -> {
            if (instance.getRarity().isImportant() && instance.getItemStack().getMaxStackSize() <= 1) {
                final UUIDProperty property = new UUIDProperty();
                final NamespacedKey key = Objects.requireNonNull(itemRegistry.getKey(instance.getBaseItem()));
                uuidManager.addUuid(new UUIDItem(property.getUniqueId(), key.getNamespace(), key.getKey()));
                return instance.withComponent(property);
            }
            return instance;
        });
    }

}