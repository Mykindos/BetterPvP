package me.mykindos.betterpvp.clans.weapons.impl.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;


/**
 * Just playing around with the idea of a handheld cannon
 */
@PluginAdapter("Champions")
@Singleton
public class HandheldCannon extends Weapon implements InteractWeapon, CooldownWeapon {

    private double velocity;

    @Inject
    public HandheldCannon(Clans clans) {
        super(clans, "handheld_cannon", "clans");
    }

    @Override
    public void activate(Player player) {
        TNTPrimed tntPrimed = player.getWorld().spawn(player.getLocation(), TNTPrimed.class);
        tntPrimed.setVelocity(player.getLocation().getDirection().multiply(velocity));
        UtilInventory.remove(player, Material.TNT, 1);
    }


    @Override
    public boolean canUse(Player player) {
        return player.getInventory().contains(Material.TNT) && isHoldingWeapon(player);
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public void loadWeaponConfig() {
        velocity = getConfig("velocity", 2.5, Double.class);
    }
}
