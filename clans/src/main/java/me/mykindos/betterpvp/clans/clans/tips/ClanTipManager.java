package me.mykindos.betterpvp.clans.clans.tips;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.tips.TipManager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

public class ClanTipManager {

    @Inject
    private Clans clans;

    @Inject
    private TipManager tipManager;

    public void load() {
        final Reflections reflections = new Reflections(getClass().getPackageName());
        final Set<Class<? extends ClanTip>> classes = reflections.getSubTypesOf(ClanTip.class);
        for (Class<? extends ClanTip> tipClass : classes) {
            if (tipClass.isInterface() || tipClass.getModifiers() == Modifier.ABSTRACT) {
                continue;
            }

            final ClanTip tip = clans.getInjector().getInstance(tipClass);
            tipManager.registerTip(clans, tip);
        }
    }
    
}
