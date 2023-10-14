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
                    
                    In the world of settings, I should be found,
                    But I'm missing, causing a puzzling sound.
                    Without me, things are chaotic, you see,
                    A configuration gone, a mystery to be.
                                        
                    I'm like a piece in a jigsaw puzzle game,
                    Without me, things will never be the same.
                    In the system's heart, where I should reside,
                    I'm absent now, causing strife far and wide.
                                        
                    What am I, this absence so dire,
                    Leaving systems and users to inquire?
                    
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
