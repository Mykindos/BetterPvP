package me.mykindos.betterpvp.progression.tree.fishing.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.tree.fishing.rod.SimpleFishingRod;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class FishingRepository implements ConfigAccessor {

    @Getter
    private final Set<FishType> fishTypes = new HashSet<>();
    @Getter
    private final Set<FishingRodType> rodTypes = new HashSet<>();
    private Database database;
    private Progression plugin;

    @Inject
    public FishingRepository(Progression plugin, Database database) {
        this.plugin = plugin;
        this.database = database;

        // Load fish types
        Reflections reflections = new Reflections(Fishing.class.getPackageName());
        Set<Class<? extends FishType>> classes = reflections.getSubTypesOf(FishType.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            FishType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);
            fishTypes.add(type);
        }
        log.info("Loaded " + fishTypes.size() + " fish types");

        Set<Class<? extends FishingRodType>> rodClasses = reflections.getSubTypesOf(FishingRodType.class);
        for (var clazz : rodClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            FishingRodType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);
            rodTypes.add(type);
        }
        rodTypes.addAll(List.of(SimpleFishingRod.values()));
        log.info("Loaded " + rodTypes.size() + " rod types");
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        fishTypes.forEach(type -> plugin.getInjector().injectMembers(type));
    }

    // todo
    //  save fish caught to database
    //  load fish caught from database

}
