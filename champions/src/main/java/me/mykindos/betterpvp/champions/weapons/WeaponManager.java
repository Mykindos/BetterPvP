package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.items.BPVPItem;
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
    }

    public void load() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends Weapon>> classes = reflections.getSubTypesOf(Weapon.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            Weapon weapon = champions.getInjector().getInstance(clazz);
            champions.getInjector().injectMembers(weapon);
            BPVPItem item = itemHandler.getItem(weapon.getIdentifier());
            if (item == null) {
                log.error(weapon.getIdentifier() + " does not exist in itemRepository");
                continue;
            }
            weapon.loadWeapon(item);
            itemHandler.replaceItem(weapon.getIdentifier(), weapon);
            addObject(weapon.getIdentifier(), weapon);
        }
        log.info("Loaded " + objects.size() + " weapons");
        champions.saveConfig();
    }

    public Optional<IWeapon> getWeaponByItemStack(ItemStack itemStack) {
        for (IWeapon weapon : objects.values()) {
            if (weapon.matches(itemStack)) return Optional.of(weapon);
        }
        return Optional.empty();
    }

}
