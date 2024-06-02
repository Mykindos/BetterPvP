package me.mykindos.betterpvp.progression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.progression.profession.IProfession;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;

@Singleton
@CustomLog
public class ProgressionsManager {

    private final Progression plugin;

    @Inject
    public ProgressionsManager(Progression plugin) {
        this.plugin = plugin;
        initProfessions();
    }

    private void initProfessions() {
        Reflections reflections = new Reflections(plugin.getClass().getPackageName());
        reflections.getSubTypesOf(IProfession.class).forEach(professionHandler -> {
            if(professionHandler.isInterface()  || Modifier.isAbstract(professionHandler.getModifiers())) return;
            try {
                var profession = plugin.getInjector().getInstance(professionHandler);
                profession.loadConfig();
            } catch (Exception e) {
                log.error("Failed to initialize profession handler: " + professionHandler.getSimpleName(), e).submit();
            }
        });
    }

}
