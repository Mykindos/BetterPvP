package me.mykindos.betterpvp.core.framework;

import com.google.inject.Injector;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public abstract class BPvPPlugin extends JavaPlugin {

    /**
     * Store our own list of listeners as spigot does not register them unless they have an active EventHandler
     */
    @Getter
    private final ArrayList<Object> listeners;

    public BPvPPlugin(){
        this.listeners = new ArrayList<>();
    }

    public abstract Injector getInjector();

}
