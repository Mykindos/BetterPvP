package me.mykindos.betterpvp.core.item.impl.cannon.ability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@Singleton
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CannonballReloadAbility extends ItemAbility {

    private final CannonManager cannonManager;

    @Inject
    public CannonballReloadAbility(Core core, CannonManager cannonManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Core.class), "cannonball_reload"), "Cannonball Reload",
                "Load a cannon with this cannonball", TriggerTypes.RIGHT_CLICK);
        this.cannonManager = cannonManager;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!canUse(player)) {
            return false;
        }

        final RayTraceResult trace = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                3,
                0.1,
                entity -> !entity.equals(player));
                
        if (trace == null) {
            return false;
        }

        final Entity targetEntity = trace.getHitEntity();
        if (targetEntity == null || !player.hasLineOfSight(trace.getHitPosition().toLocation(player.getWorld()))) {
            return false;
        }

        final Optional<Cannon> cannonOpt = this.cannonManager.of(targetEntity);
        if (cannonOpt.isEmpty()) {
            return false;
        }

        final Cannon cannon = cannonOpt.get();
        if (cannon.isLoaded()) {
            return false;
        }

        final CannonReloadEvent cannonReloadEvent = new CannonReloadEvent(cannon, player);
        cannonReloadEvent.callEvent();
        if (!cannonReloadEvent.isCancelled()) {
            return true;
        }
        return false;
    }
    
    private boolean canUse(Player player) {
        if (!Compatibility.MODEL_ENGINE) {
            UtilMessage.message(player, "Combat", "Cannons are not supported on this server. <red>Please contact an administrator.");
            return false;
        }
        return true;
    }
} 