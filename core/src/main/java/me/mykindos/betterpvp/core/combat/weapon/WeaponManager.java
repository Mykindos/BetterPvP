package me.mykindos.betterpvp.core.combat.weapon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import org.bukkit.inventory.ItemStack;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Optional;

@Singleton
@Slf4j
public class WeaponManager extends Manager<IWeapon> {

    private final Core core;
    private final ItemHandler itemHandler;

    @Inject
    public WeaponManager(Core core, ItemHandler itemHandler) {
        this.core = core;
        this.itemHandler = itemHandler;
    }

    public void load() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        Adapters adapters = new Adapters(core);
        reflections.getSubTypesOf(Weapon.class).forEach(clazz -> load(core, adapters, clazz));
        log.info("Loaded " + objects.size() + " weapons");
    }

    public boolean load(Weapon weapon) {
        BPVPItem item = itemHandler.getItem(weapon.getIdentifier());
        if (item == null) {
            log.error(weapon.getIdentifier() + " does not exist in itemRepository");
            return false;
        }
        weapon.loadWeapon(item);
        itemHandler.replaceItem(weapon.getIdentifier(), weapon);
        addObject(weapon.getIdentifier(), weapon);
        return true;
    }

    public boolean load(BPvPPlugin plugin, Adapters adapters, Class<? extends Weapon> clazz) {
        if (!adapters.canLoad(clazz)) return false; // Check if the adapter can be loaded (if it has the PluginAdapter annotation)
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) return false;
        if (clazz.isAnnotationPresent(Deprecated.class)) return false;
        Weapon weapon = plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(weapon);
        return load(weapon);
    }

    public Optional<IWeapon> getWeaponByItemStack(ItemStack itemStack) {
        for (IWeapon weapon : objects.values()) {
            if (weapon.matches(itemStack)) return Optional.of(weapon);
        }
        return Optional.empty();
    }

}
