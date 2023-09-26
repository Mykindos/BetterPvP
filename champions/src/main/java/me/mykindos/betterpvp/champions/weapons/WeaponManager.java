package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.Material;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

@Singleton
@Slf4j
public class WeaponManager extends Manager<IWeapon> {

    private final Champions champions;

    @Inject
    public WeaponManager(Champions champions) {
        this.champions = champions;

        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends Weapon>> classes = reflections.getSubTypesOf(Weapon.class);
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            Weapon weapon = champions.getInjector().getInstance(clazz);
            champions.getInjector().injectMembers(weapon);

            addObject(weapon.getMaterial().name(), weapon);

        }

        log.info("Loaded " + objects.size() + " weapons");
        champions.saveConfig();
    }

    public Optional<IWeapon> getWeaponByType(Material material) {
        return getObject(material.name());
    }

}
