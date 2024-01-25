package me.mykindos.betterpvp.clans.weapons.impl.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Optional;

@PluginAdapter("ModelEngine")
@Singleton
public class CannonballWeapon extends Weapon implements InteractWeapon {

    private final CannonManager cannonManager;

    @Inject
    public CannonballWeapon(CannonManager cannonManager) {
        super("clans", "cannonball");
        this.cannonManager = cannonManager;
    }

    @Override
    public void activate(Player player) {
        final RayTraceResult trace = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                3,
                0.1,
                entity -> !entity.equals(player));
        if (trace == null) {
            return;
        }

        final Entity targetEntity = trace.getHitEntity();
        if (targetEntity == null || !player.hasLineOfSight(trace.getHitPosition().toLocation(player.getWorld()))) {
            return;
        }

        final Optional<Cannon> cannonOpt = this.cannonManager.of(targetEntity);
        if (cannonOpt.isEmpty()) {
            return;
        }

        final Cannon cannon = cannonOpt.get();
        if (cannon.isLoaded()) {
            return;
        }

        final CannonReloadEvent cannonReloadEvent = new CannonReloadEvent(cannon, player);
        cannonReloadEvent.callEvent();
        if (!cannonReloadEvent.isCancelled()) {
            UtilInventory.remove(player, getMaterial(), 1);
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (!Compatibility.MODEL_ENGINE) {
            UtilMessage.message(player, "Combat", "Cannons are not supported on this server. <red>Please contact an administrator.");
            return false;
        }
        return true;
    }

}
