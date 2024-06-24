package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.progression.Progression;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

@Singleton
@CustomLog
public class ProgressionSkillManager extends Manager<ProgressionSkill> {

    private final Progression progression;

    @Inject
    public ProgressionSkillManager(Progression progression) {
        this.progression = progression;
    }

    /**
     * Load all skills in the classpath if theyre enabled in the config
     * All skills are enabled by default
     */
    public void loadSkills(){
        Adapters adapters = new Adapters(progression);
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends ProgressionSkill>> classes = reflections.getSubTypesOf(ProgressionSkill.class);
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            if(!adapters.canLoad(clazz)) continue;
            ProgressionSkill skill = progression.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(skill);

            addObject(skill.getName(), skill);

        }

        log.info("Loaded " + objects.size() + " skills").submit();
        progression.saveConfig();
    }

    public void reloadSkills(){
        getObjects().values().forEach(ProgressionSkill::reload);
    }

    public Optional<ProgressionSkill> getSkill(String name){
        return objects.values().stream().filter(skill -> skill.getName().equalsIgnoreCase(name)).findFirst();
    }

}
