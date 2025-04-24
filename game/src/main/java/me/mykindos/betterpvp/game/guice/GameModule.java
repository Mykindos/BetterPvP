package me.mykindos.betterpvp.game.guice;

import com.google.inject.Module;
import org.bukkit.event.Listener;

import java.util.Set;

/**
 * Represents a game module that can be dynamically loaded/unloaded.
 */
public interface GameModule extends Module {
    
    /**
     * @return The unique identifier for this module
     */
    String getId();
    
    /**
     * @return Set of listeners that should be registered when this module is loaded
     */
    Set<Class<? extends Listener>> getListeners();
    
    /**
     * Called when the module is being loaded
     */
    void onEnable();
    
    /**
     * Called when the module is being unloaded
     */
    void onDisable();

    /**
     * @return The scope of this module
     */
    GameScope getScope();
}