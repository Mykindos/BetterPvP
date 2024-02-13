package me.mykindos.betterpvp.clans.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.reflections.Reflections;

@PluginAdapter("Core")
public class ClansWeaponManager {

    private final WeaponManager weaponManager;
    private final Adapters adapters;
    private final Clans clans;

    @Inject
    public ClansWeaponManager(WeaponManager weaponManager, Clans clans) {
        this.weaponManager = weaponManager;
        this.adapters = new Adapters(clans);
        this.clans = clans;
    }

    public void load() {
        new Reflections(getClass().getPackageName()).getSubTypesOf(Weapon.class).forEach(clazz -> weaponManager.load(clans, adapters, clazz));
    }

    public void reload() {
        weaponManager.reload(clans);
    }

}

