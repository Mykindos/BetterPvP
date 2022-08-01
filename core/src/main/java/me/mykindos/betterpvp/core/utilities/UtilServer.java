package me.mykindos.betterpvp.core.utilities;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public class UtilServer {

    /**
     * Shorter version of doing Bukkit.getPluginManager().callEvent()
     * @param event The event to call
     */
    public static void callEvent(Event event){
        Bukkit.getPluginManager().callEvent(event);
    }

}
