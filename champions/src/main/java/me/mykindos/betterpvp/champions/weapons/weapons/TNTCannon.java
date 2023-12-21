package me.mykindos.betterpvp.champions.weapons.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.champions.weapons.types.CooldownWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;


/**
 * Just playing around with the idea of a handheld cannon
 */
@Singleton
@BPvPListener
public class TNTCannon extends Weapon implements Listener, InteractWeapon, CooldownWeapon {

    @Inject
    @Config(path = "weapons.tnt-cannon.cooldown", defaultValue = "25.0")
    private double cooldown;

    @Inject
    @Config(path = "weapons.tnt-cannon.velocity", defaultValue = "2.5")
    private double velocity;

    @Inject
    public TNTCannon(ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(Material.CARROT_ON_A_STICK, 1, Component.text("TNT Cannon", NamedTextColor.LIGHT_PURPLE));

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

}
