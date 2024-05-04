package me.mykindos.betterpvp.champions.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

@CustomLog
@Singleton
public class SkillManager extends Manager<Skill> {

    private final Champions champions;

    @Inject
    public SkillManager(Champions champions){
        this.champions = champions;
    }

    /**
     * Load all skills in the classpath if theyre enabled in the config
     * All skills are enabled by default
     */
    public void loadSkills(){
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends Skill>> classes = reflections.getSubTypesOf(Skill.class);
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            Skill skill = champions.getInjector().getInstance(clazz);
            champions.getInjector().injectMembers(skill);

            addObject(skill.getName(), skill);

        }

        log.info("Loaded " + objects.size() + " skills").submit();
        champions.saveConfig();
    }

    public void reloadSkills(){
        getObjects().values().forEach(Skill::reload);
    }

    public List<Skill> getSkillsForRole(Role role) {
        return objects.values().stream().filter(skill -> skill.getClassType() == role || skill.getType() == SkillType.GLOBAL).toList();
    }

}
