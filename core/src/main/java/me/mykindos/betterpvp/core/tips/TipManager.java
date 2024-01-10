package me.mykindos.betterpvp.core.tips;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.Collection;

@Slf4j
@Singleton
public class TipManager extends Manager<Tip> {

    public void registerTip(BPvPPlugin plugin, Tip tip) {
        addObject(tip.getName(), tip);
        plugin.saveConfig();
        log.info("Loaded Tip " + tip.getName());
    }

    public void reloadTips() {
        getObjects().values().forEach(Tip::loadConfig);
    }

    public Collection<? extends Tip> getTips() {
        return getObjects().values();
    }


}
