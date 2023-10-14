package me.mykindos.betterpvp.clans.progression;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.ProgressionsManager;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;

@PluginAdapter("Progression")
@Slf4j
public class ProgressionAdapter{

    private final Clans clans;
    private final Progression progression;
    private final ProgressionsManager progressionsManager;
    private final ClansListenerLoader listenerLoader;

    @Inject
    public ProgressionAdapter(Clans clans, ClansListenerLoader listenerLoader) {
        this.clans = clans;
        this.listenerLoader = listenerLoader;
        this.progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.progressionsManager = progression.getProgressionsManager();

        load();
    }

    public void load() {
        loadListeners();
        loadPerks();
    }

    private void loadListeners() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        final Set<Class<? extends Listener>> listenerClasses = reflections.getSubTypesOf(Listener.class);
        for (Class<? extends Listener> clazz : listenerClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            final Listener listener = clans.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(listener);
            listenerLoader.load(clazz);
        }
        log.info("Loaded " + listenerClasses.size() + " clans progression listeners");
    }

    private void loadPerks() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        final Set<Class<? extends ProgressionPerk>> perkClasses = reflections.getSubTypesOf(ProgressionPerk.class);
        for (Class<? extends ProgressionPerk> clazz : perkClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            final ProgressionPerk perk = clans.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(perk);
            clans.getInjector().injectMembers(perk);
            final Class<? extends ProgressionTree>[] trees = perk.acceptedTrees();
            for (Class<? extends ProgressionTree> tree : trees) {
                progressionsManager.fromClass(tree).addPerk(perk);
            }
        }
        log.info("Loaded " + perkClasses.size() + " clans progression perks");
    }

}
