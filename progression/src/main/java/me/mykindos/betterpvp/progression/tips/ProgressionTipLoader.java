package me.mykindos.betterpvp.progression.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.tips.TipLoader;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.progression.Progression;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

@Singleton
public class ProgressionTipLoader extends TipLoader {

    @Inject
    public ProgressionTipLoader(Progression plugin, TipManager tipManager) {
        super(plugin, tipManager);
    }

    public void loadTips(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<? extends ProgressionTip>> classes = reflections.getSubTypesOf(ProgressionTip.class);
        for (Class<? extends ProgressionTip> tipClass : classes) {
            if (tipClass.isInterface() || tipClass.getModifiers() == Modifier.ABSTRACT) {
                continue;
            }

            load(tipClass);
        }
    }

}
