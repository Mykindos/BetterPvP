package me.mykindos.betterpvp.clans.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.tips.TipLoader;
import me.mykindos.betterpvp.core.tips.TipManager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

@Singleton
public class ClansTipLoader extends TipLoader {

    @Inject
    public ClansTipLoader(Clans plugin, TipManager tipManager) {
        super(plugin, tipManager);
    }

    public void loadTips(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<? extends ClanTip>> classes = reflections.getSubTypesOf(ClanTip.class);
        for (Class<? extends ClanTip> tipClass : classes) {
            if (tipClass.isInterface() || tipClass.getModifiers() == Modifier.ABSTRACT) {
                continue;
            }

            load(tipClass);
        }
    }

}
