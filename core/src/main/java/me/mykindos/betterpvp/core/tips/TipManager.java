package me.mykindos.betterpvp.core.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

@Slf4j
@Singleton
public class TipManager extends Manager<Tip> {
    private final Core core;

    @Inject
    public TipManager (Core core) {
        this.core = core;
    }

    public void loadTips() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends Tip>> classes = reflections.getSubTypesOf(Tip.class);
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            Tip tip = core.getInjector().getInstance(clazz);
            core.getInjector().injectMembers(tip);

            addObject(tip.getName(), tip);

        }

        log.info("Loaded " + objects.size() + " TIPS TIPS TIPS TIPS");
        core.saveConfig();
    }

    public void reloadTips() {
        getObjects().values().forEach(tip -> {
            core.getInjector().injectMembers(tip);
        });
    }

    public Collection<? extends Tip> getTips() {
        return getObjects().values();
    }


}
