package me.mykindos.betterpvp.clans.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

import java.util.Set;

@Singleton
public class SkillManager extends Manager<ISkill> {

    private final Clans clans;

    @Inject
    public SkillManager(Clans clans){
        this.clans = clans;
    }

    /**
     * Load all skills in the classpath if theyre enabled in the config
     * All skills are enabled by default
     */
    public void loadSkills(){
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends Skill>> classes = reflections.getSubTypesOf(Skill.class);
        for (var clazz : classes) {
            if(clazz.isInterface()) continue;
            Skill skill = clans.getInjector().getInstance(clazz);
            clans.getInjector().injectMembers(skill);

            var skillPath = skill.getClassType() + "." + skill.getName() + ".enabled";
            var set = clans.getConfig().isSet(skillPath.toLowerCase());
            if(!set){
                clans.getConfig().set(skillPath.toLowerCase(), true);
            }

            skill.setEnabled(clans.getConfig().getBoolean(skillPath.toLowerCase()));
            addObject(skill.getName(), skill);

        }

        System.out.println("Loaded " + objects.size() + " skills");
        clans.saveConfig();
    }

    /**
     * Reload all skills from the classpath
     * Useful if configuration has changed to change skill settings
     */
    public void reloadSkills(){
        getObjects().values().forEach(skill -> {
            clans.getInjector().injectMembers(skill);
        });
    }
}
