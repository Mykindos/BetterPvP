package me.mykindos.betterpvp.clans;

import com.google.inject.Injector;
import lombok.Getter;
import me.mykindos.betterpvp.clans.injector.ClansInjectorModule;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.core.Core;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import org.bukkit.Bukkit;


public class Clans extends BPvPPlugin {

    @Getter
    private Injector injector;

    @Override
    public void onEnable() {

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            injector = core.getInjector().createChildInjector(new ClansInjectorModule(this));
            injector.injectMembers(this);

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Clans"));

            ClansListenerLoader clansListenerLoader = new ClansListenerLoader(this);
            clansListenerLoader.registerListeners("me.mykindos.betterpvp.clans");

        }
    }
}
