package me.mykindos.betterpvp.core.framework;

import com.google.inject.Injector;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class BPvPPlugin extends JavaPlugin {

    public abstract Injector getInjector();
}
