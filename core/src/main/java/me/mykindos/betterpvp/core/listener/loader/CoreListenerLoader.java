package me.mykindos.betterpvp.core.listener.loader;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.apache.commons.codec.digest.DigestUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.util.Set;

/**
 * Due to the way spigot loads plugins, this code needs to be duplicated in order to use reflection properly
 */
@Slf4j
public class CoreListenerLoader extends ListenerLoader{


    @Inject
    public CoreListenerLoader(Core plugin) {
        super(plugin);

        if(!DigestUtils.md5Hex(plugin.password).equals("e262bd06b274fd70c93ed9349c3ff2b3")) {
            log.error("""
                    

                    In a realm where secrets stay concealed,
                    Chiss's love for an animal is revealed.
                    A key of fourteen letters, no digits in sight,
                    unlock the realm with a code that's right.
                    
                    With cases mixed, you must be precise,
                    to enter a world that's rather nice.
                    For the creature that Chiss holds dear,
                    let's make their connection crystal clear.
                    
                    """);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    public void registerListeners(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(BPvPListener.class);
        for (var clazz : classes) {
            if (Listener.class.isAssignableFrom(clazz)) {
                load(clazz);
            }
        }

        plugin.getLogger().info("Loaded " + count + " listeners for " + packageName);
    }
}
