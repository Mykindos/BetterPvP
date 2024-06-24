package me.mykindos.betterpvp.clans.progression;

import com.google.inject.Inject;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;

@PluginAdapter("Progression")
@CustomLog
public class ProgressionAdapter {

    @Getter
    private final Progression progression;
    private final Clans clans;
    private final ClansListenerLoader listenerLoader;

    @Inject
    public ProgressionAdapter(Clans clans, ClansListenerLoader listenerLoader) {
        this.clans = clans;
        this.listenerLoader = listenerLoader;
        this.progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));

    }

    public void load() {
        loadListeners();
    }

    private void loadListeners() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        final Set<Class<? extends Listener>> listenerClasses = reflections.getSubTypesOf(Listener.class);
        for (Class<? extends Listener> clazz : listenerClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            final Listener listener = clans.getInjector().getInstance(clazz);
            if (listener instanceof ConfigAccessor accessor) {
                accessor.loadConfig(clans.getConfig());
            }

            listenerLoader.load(clazz);
        }
        log.info("Loaded " + listenerClasses.size() + " clans progression listeners").submit();
    }


}
