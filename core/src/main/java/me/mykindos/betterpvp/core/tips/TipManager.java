package me.mykindos.betterpvp.core.tips;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.Collection;

@CustomLog
@Singleton
public class TipManager extends Manager<Tip> {

    public void registerTip(BPvPPlugin plugin, Tip tip) {
        addObject(tip.getName(), tip);
        plugin.saveConfig();
        log.info("Loaded Tip " + tip.getName());
    }

    public void reloadTips(BPvPPlugin plugin) {

        getObjects().values().forEach(tip -> {
            tip.loadConfig(plugin);
        });
    }

    public Collection<? extends Tip> getTips() {
        return getObjects().values();
    }


}
