package me.mykindos.betterpvp.champions.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.traits.Trait;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@CustomLog
@Singleton
public class ChampionsSkillManager extends Manager<String, Skill> {

    private final Champions champions;
    private final CooldownManager cooldownManager;

    @Inject
    public ChampionsSkillManager(Champions champions, CooldownManager cooldownManager){
        this.champions = champions;
        this.cooldownManager = cooldownManager;
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

            // Register the skill's translatable display name so cooldowns (action bar + chat) localize it
            // instead of showing the raw internal name.
            cooldownManager.registerDisplayName(skill.getName(), skill.getDisplayName());

        }

        log.info("Loaded " + objects.size() + " skills").submit();
        champions.saveConfig();
    }

    public void reloadSkills(){
        getObjects().values().forEach(Skill::reload);
    }

    /**
     * The skills a player of this role can put into a build. Traits are excluded: they belong to the role
     * rather than to a build, so they are never selectable.
     *
     * @param role The role
     * @return The selectable skills for the role
     */
    public List<Skill> getSkillsForRole(Role role) {
        return objects.values().stream()
                .filter(skill -> skill.getType().isSelectable())
                .filter(skill -> skill.getClassType() == role || skill.getType() == SkillType.GLOBAL)
                .toList();
    }

    /**
     * The traits innately held by every player of this role, in a stable display order.
     *
     * @param role The role
     * @return The enabled traits for the role
     */
    public List<Trait> getTraitsForRole(Role role) {
        return objects.values().stream()
                .filter(Trait.class::isInstance)
                .map(Trait.class::cast)
                .filter(trait -> trait.getClassType() == role)
                .filter(Trait::isEnabled)
                .sorted(Comparator.comparing(Trait::getName))
                .toList();
    }

}
