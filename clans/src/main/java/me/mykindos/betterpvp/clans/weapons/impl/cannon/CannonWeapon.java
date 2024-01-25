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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PluginAdapter("ModelEngine")
@Singleton
public class CannonWeapon extends Weapon implements InteractWeapon, CooldownWeapon {

    private final CannonManager cannonManager;
    private final Map<UUID, Location> cannonLocations = new HashMap<>();

    @Inject
    public CannonWeapon(CannonManager cannonManager) {
        super("clans", "cannon");
        this.cannonManager = cannonManager;
    }

    @Override
    public void activate(Player player) {
        final Location cannonLocation = cannonLocations.remove(player.getUniqueId());
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
            SoundEffect.LOW_PITCH_PLING.play(player);
        } else {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(4.5, FluidCollisionMode.ALWAYS);
            if (rayTraceResult == null || rayTraceResult.getHitBlock() == null) {
                event.cancel("No block in sight.");
            } else {
                final Location location = rayTraceResult.getHitPosition().toLocation(player.getWorld());
                location.setYaw(player.getLocation().getYaw());
                location.setPitch(0f);
                cannonLocations.put(player.getUniqueId(), location);
            }
        }
        event.callEvent();
        return !event.isCancelled();
    }

    @Override
    public double getCooldown() {
        return cannonManager.getSpawnCooldown();
    }

}
