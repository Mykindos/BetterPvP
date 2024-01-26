package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.reflections.Reflections;

@PluginAdapter("Core")
public class ChampionsWeaponManager {

    private final WeaponManager weaponManager;
    private final Adapters adapters;
    private final Champions champions;

    @Inject
    public ChampionsWeaponManager(WeaponManager weaponManager, Champions champions) {
        this.weaponManager = weaponManager;
        this.adapters = new Adapters(champions);
        this.champions = champions;
    }

    public void load() {
        new Reflections(getClass().getPackageName()).getSubTypesOf(Weapon.class).forEach(clazz -> weaponManager.load(champions, adapters, clazz));
    }

}
