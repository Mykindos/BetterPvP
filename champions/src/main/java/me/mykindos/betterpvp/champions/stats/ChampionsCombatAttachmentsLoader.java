package me.mykindos.betterpvp.champions.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Modifier;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsStatsRepository;
import me.mykindos.betterpvp.core.combat.stats.model.IAttachmentLoader;
import me.mykindos.betterpvp.core.framework.Loader;
import org.reflections.Reflections;


@Singleton
@CustomLog
public class ChampionsCombatAttachmentsLoader extends Loader {
    private final ChampionsStatsRepository championsStatsRepository;
    @Inject
    public ChampionsCombatAttachmentsLoader(Champions plugin, ChampionsStatsRepository championsStatsRepository) {
        super(plugin);
        this.championsStatsRepository = championsStatsRepository;
    }

    public void loadAll(Set<Class<? extends IAttachmentLoader>> classes) {
        for (var clazz : classes) {
            if (IAttachmentLoader.class.isAssignableFrom(clazz)) {
                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    load(clazz);
                }
            }
        }
    }

    public void loadAttachments(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAttachmentLoader>> classes = reflections.getSubTypesOf(IAttachmentLoader.class);
        loadAll(classes);

        plugin.saveConfig();
        log.info("Loaded {} combat attachments for Champions", count).submit();
    }



    @Override
    public void load(Class<?> clazz) {
        IAttachmentLoader attachmentLoader = (IAttachmentLoader) plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(attachmentLoader);

        championsStatsRepository.addAttachmentLoader(attachmentLoader);
        count++;

    }
}
