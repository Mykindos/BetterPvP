package me.mykindos.betterpvp.clans.progression;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.ProgressionsManager;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

public class ProgressionAdapter {

    private final Clans clans;
    private final Progression progression;
    private final ProgressionsManager progressionsManager;
    private final ClansListenerLoader listenerLoader;
    
    public ProgressionAdapter(Clans clans, BPvPPlugin progressions, ClansListenerLoader listenerLoader) {
        this.clans = clans;
        this.listenerLoader = listenerLoader;
        this.progression = (Progression) progressions;
        this.progressionsManager = progression.getProgressionsManager();
    }

    public void load() {
        loadPerks();
    }

    private void loadPerks() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        final Set<Class<? extends ProgressionPerk>> perkClasses = reflections.getSubTypesOf(ProgressionPerk.class);
        for (Class<? extends ProgressionPerk> clazz : perkClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            final ProgressionPerk perk = clans.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(perk);
            final Class<? extends ProgressionTree>[] trees = perk.acceptedTrees();
            for (Class<? extends ProgressionTree> tree : trees) {
                progressionsManager.fromClass(tree).addPerk(perk);
            }

            if (Listener.class.isAssignableFrom(clazz)) {
                listenerLoader.load(clazz);
            }
        }

    }

}
