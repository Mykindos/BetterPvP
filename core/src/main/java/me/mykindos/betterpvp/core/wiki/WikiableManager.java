package me.mykindos.betterpvp.core.wiki;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.wiki.types.IWikiable;
import me.mykindos.betterpvp.core.wiki.types.WikiCategory;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.List;

@Singleton
@CustomLog
public class WikiableManager extends Manager<IWikiable> {

    private final Core core;
    @Inject
    public WikiableManager(Core core) {
        this.core = core;
    }

    public void load() {
        Reflections reflections = new Reflections(core.getClass().getPackageName());
        Adapters adapters = new Adapters(core);
        reflections.getSubTypesOf(IWikiable.class).forEach(clazz -> load(core, adapters, clazz));
        log.info("Loaded " + objects.size() + " wikiables").submit();
    }

    public boolean load(BPvPPlugin plugin, Adapters adapters, Class<? extends IWikiable> clazz) {
        log.info(clazz.getCanonicalName());
        if (!adapters.canLoad(clazz)) return false; // Check if the adapter can be loaded (if it has the PluginAdapter annotation)
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) return false;
        if (clazz.isAnnotationPresent(Deprecated.class)) return false;
        IWikiable wikiable = plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(wikiable);
        addObject(wikiable.getName(), wikiable);
        log.info("Loaded Wikiable" + wikiable.getName());
        return true;
    }

    public List<IWikiable> getWikiablesForCategory(WikiCategory wikiCategory) {
        return getObjects().values().stream().filter(iWikiable -> iWikiable.getCategory().equals(wikiCategory)).toList();
    }

}
