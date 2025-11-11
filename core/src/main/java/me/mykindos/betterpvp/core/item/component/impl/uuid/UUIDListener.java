package me.mykindos.betterpvp.core.item.component.impl.uuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class UUIDListener implements Listener {

    @Inject
    private UUIDListener(ItemFactory itemFactory) {
        itemFactory.registerDefaultBuilder(instance -> {
            if (instance.getRarity().isImportant() && instance.getItemStack().getMaxStackSize() <= 1) {
                return instance.withComponent(new UUIDProperty());
            }
            return instance;
        });
    }

}