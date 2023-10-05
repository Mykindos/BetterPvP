package me.mykindos.betterpvp.progression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class ProgressionsManager {

    private final List<ProgressionTree> trees = new ArrayList<>();

    private final Progression progression;

    @Inject
    public ProgressionsManager(Progression progression) {
        this.progression = progression;
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends ProgressionTree>> classes = reflections.getSubTypesOf(ProgressionTree.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            ProgressionTree tree = progression.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(tree);

            trees.add(tree);
        }

        log.info("Loaded " + trees.size() + " skills");
        progression.saveConfig();
    }

    public ProgressionTree fromClass(Class<? extends ProgressionTree> clazz) {
        return trees.stream().filter(clazz::isInstance).findFirst().orElseThrow();
    }

    public List<ProgressionTree> getTrees() {
        return trees;
    }

    public void loadTrees() {
        trees.forEach(tree -> tree.loadConfig(progression.getConfig()));
    }

}
