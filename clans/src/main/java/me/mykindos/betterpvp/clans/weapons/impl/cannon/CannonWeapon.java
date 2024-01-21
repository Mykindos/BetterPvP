package me.mykindos.betterpvp.clans.weapons.impl.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonPlaceEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.PreCannonPlaceEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@PluginAdapter("ModelEngine")
@Singleton
public class CannonWeapon extends Weapon implements InteractWeapon, CooldownWeapon {

    private final CannonManager cannonManager;

    @Inject
    public CannonWeapon(CannonManager cannonManager) {
        super("clans", "cannon");
        this.cannonManager = cannonManager;
    }

    @Override
    public void activate(Player player) {
        final Location cannonLocation = player.getLocation();
        cannonLocation.setPitch(0f);
        final Cannon cannon = this.cannonManager.spawn(player.getUniqueId(), cannonLocation);
        final CannonPlaceEvent event = new CannonPlaceEvent(cannon, cannonLocation, player);
        event.callEvent();

        final ItemStack main = player.getInventory().getItemInMainHand();
        final ItemStack off = player.getInventory().getItemInOffHand();
        if (matches(main)) {
            main.subtract();
            player.getInventory().setItemInMainHand(main);
        } else if (matches(off)) {
            off.subtract();
            player.getInventory().setItemInOffHand(off);
        }

        UtilMessage.message(player, "Combat", "You placed a <alt2>Cannon</alt2>!");
    }

    @Override
    public boolean canUse(Player player) {
        final PreCannonPlaceEvent event = new PreCannonPlaceEvent(player.getLocation(), player);
        if (!Compatibility.MODEL_ENGINE) {
            event.cancel("Cannons are not supported on this server. There are missing dependencies.");
            UtilMessage.message(player, "Combat", "Cannons are not supported on this server. <red>Please contact an administrator.");
        } else if (!UtilBlock.isGrounded(player)) {
            event.cancel("Not Grounded");
            UtilMessage.message(player, "Combat", "You must be standing on the ground to place a cannon.");
        }
        event.callEvent();
        return !event.isCancelled();
    }

    @Override
    public double getCooldown() {
        return cannonManager.getSpawnCooldown();
    }

}
