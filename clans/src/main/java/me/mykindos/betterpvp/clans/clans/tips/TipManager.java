package me.mykindos.betterpvp.clans.clans.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class TipManager extends Manager<Tip> {
    private final Clans clans;

    @Inject
    public TipManager (Clans clans) {
        this.clans = clans;
    }

    public void loadTips() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends Tip>> classes = reflections.getSubTypesOf(Tip.class);
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            Tip tip = clans.getInjector().getInstance(clazz);
            clans.getInjector().injectMembers(tip);

            addObject(tip.getName(), tip);

        }

        log.info("Loaded " + objects.size() + " tips");
        clans.saveConfig();
    }

    public void reloadTips() {
        getObjects().values().forEach(tip -> {
            clans.getInjector().injectMembers(tip);
        });
    }

    public Collection<? extends Tip> getTips() {
        return getObjects().values();
    }


}
