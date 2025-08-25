package me.mykindos.betterpvp.core.block.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.TickHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class SmartBlockTicker implements Listener {

    private final SmartBlockDataManager dataManager;

    @Inject
    private SmartBlockTicker(SmartBlockDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @UpdateEvent
    public void onUpdate() {
        for (SmartBlockData<?> smartBlockData : dataManager.getProvider().collectAll()) {
            final Object data = smartBlockData.get();
            final SmartBlockInstance instance = smartBlockData.getBlockInstance();
            if (data instanceof TickHandler tickHandler) {
                tickHandler.onTick(instance);
            }
        }
    }
} 