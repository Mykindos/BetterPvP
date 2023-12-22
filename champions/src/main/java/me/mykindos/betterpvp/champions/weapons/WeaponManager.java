package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.items.ItemHandler;
import org.bukkit.inventory.ItemStack;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

@Singleton
@Slf4j
public class WeaponManager extends Manager<IWeapon> {

    private final Champions champions;
    private final ItemHandler itemHandler;

    @Inject
    public WeaponManager(Champions champions, ItemHandler itemHandler) {
        this.champions = champions;
        this.itemHandler = itemHandler;

        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends Weapon>> classes = reflections.getSubTypesOf(Weapon.class);
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            Weapon weapon = champions.getInjector().getInstance(clazz);
            champions.getInjector().injectMembers(weapon);
            weapon.loadWeapon(itemHandler.getItemMap().get(weapon.getIdentifier()));
            addObject(weapon.getIdentifier(), weapon);
        }

        log.info("Loaded " + objects.size() + " weapons");
        champions.saveConfig();
    }

    public void reload() {
        for (IWeapon weapon : getObjects().values()) {
            champions.getInjector().injectMembers(weapon);
        }
    }

    public Optional<IWeapon> getWeaponByItemStack(ItemStack itemStack) {
        var meta = itemStack.getItemMeta();
        return getObject(itemStack.getType().name() + (meta.hasCustomModelData() ? meta.getCustomModelData() : 0));
    }

}
