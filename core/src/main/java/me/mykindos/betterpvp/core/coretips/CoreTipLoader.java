package me.mykindos.betterpvp.core.coretips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.tips.TipLoader;
import me.mykindos.betterpvp.core.tips.TipManager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

@Singleton
public class CoreTipLoader extends TipLoader {

    @Inject
    public CoreTipLoader(Core plugin, TipManager tipManager) {
        super(plugin, tipManager);
    }

    public void loadTips(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<? extends CoreTip>> classes = reflections.getSubTypesOf(CoreTip.class);
        for (Class<? extends CoreTip> tipClass : classes) {
            if (tipClass.isInterface() || tipClass.getModifiers() == Modifier.ABSTRACT) {
                continue;
            }

            load(tipClass);
        }
    }

}
