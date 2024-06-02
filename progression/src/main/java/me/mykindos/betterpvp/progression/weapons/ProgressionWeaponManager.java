package me.mykindos.betterpvp.progression.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.progression.Progression;
import org.reflections.Reflections;

@Singleton
public class ProgressionWeaponManager {

    private final WeaponManager weaponManager;
    private final Adapters adapters;
    private final Progression progression;

    @Inject
    public ProgressionWeaponManager(WeaponManager weaponManager, Progression progression) {
        this.weaponManager = weaponManager;
        this.adapters = new Adapters(progression);
        this.progression = progression;
    }

    public void load() {
        new Reflections(progression.getClass().getPackageName()).getSubTypesOf(Weapon.class).forEach(clazz -> {
            weaponManager.load(progression, adapters, clazz);
        });
    }

    public void reload() {
        weaponManager.reload(progression);
    }


}
