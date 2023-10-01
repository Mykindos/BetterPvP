package me.mykindos.betterpvp.core.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

@Slf4j
@Singleton
public class TipManager extends Manager<Tip> {

    public void loadTips(BPvPPlugin plugin, String packageName, Class type) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends Tip>> classes = reflections.getSubTypesOf(type);
        log.info(classes.toString());
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            Tip tip = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(tip);

            addObject(tip.getName(), tip);

        }

        log.info("Loaded " + objects.size() + " TIPS TIPS TIPS TIPS");
        plugin.saveConfig();
    }

    public void reloadTips(BPvPPlugin plugin) {
        getObjects().values().forEach(tip -> {
            plugin.getInjector().injectMembers(tip);
        });
    }

    public Collection<? extends Tip> getTips() {
        return getObjects().values();
    }


}
