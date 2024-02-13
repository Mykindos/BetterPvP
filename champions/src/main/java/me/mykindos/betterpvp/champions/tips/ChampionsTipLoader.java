package me.mykindos.betterpvp.champions.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.tips.TipLoader;
import me.mykindos.betterpvp.core.tips.TipManager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

@Singleton
public class ChampionsTipLoader extends TipLoader {

    @Inject
    public ChampionsTipLoader(Champions plugin, TipManager tipManager) {
        super(plugin, tipManager);
    }

    public void loadTips(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<? extends ChampionsTip>> classes = reflections.getSubTypesOf(ChampionsTip.class);
        for (Class<? extends ChampionsTip> tipClass : classes) {
            if (tipClass.isInterface() || tipClass.getModifiers() == Modifier.ABSTRACT) {
                continue;
            }

            load(tipClass);
        }
    }

}
