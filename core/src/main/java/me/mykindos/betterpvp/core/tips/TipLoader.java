package me.mykindos.betterpvp.core.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;

@Singleton
public class TipLoader extends Loader {

    private final TipManager tipManager;

    @Inject
    public TipLoader(BPvPPlugin plugin, TipManager tipManager) {
        super(plugin);
        this.tipManager = tipManager;
    }

    @Override
    public void load(Class<?> clazz) {
        var tip = (Tip) plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(tip);
        tipManager.registerTip(plugin, tip);
    }

}
