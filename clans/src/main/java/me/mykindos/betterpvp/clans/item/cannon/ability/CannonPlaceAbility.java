package me.mykindos.betterpvp.clans.item.cannon.ability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.item.cannon.event.PreCannonPlaceEvent;
import me.mykindos.betterpvp.clans.item.cannon.model.Cannon;
import me.mykindos.betterpvp.clans.item.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Singleton
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CannonPlaceAbility extends ItemAbility {

    @EqualsAndHashCode.Include
    private double cooldown;
    private final CannonManager cannonManager;
    private final Map<UUID, Location> cannonLocations = new HashMap<>();
    private final CooldownManager cooldownManager;

    @Inject
    private CannonPlaceAbility(Clans clans, CannonManager cannonManager, CooldownManager cooldownManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Clans.class), "cannon_place"), "Cannon Placement",
                "Place a cannon that can be loaded with cannonballs", TriggerTypes.RIGHT_CLICK);
        this.cannonManager = cannonManager;
        this.cooldown = cannonManager.getSpawnCooldown();
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!canUse(player)) {
            return false;
        }

        if (!cooldownManager.use(player, getName(), cooldown, true, true)) {
            return false;
        }

        final Location cannonLocation = cannonLocations.remove(player.getUniqueId());

        if(!cannonLocation.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {
            UtilMessage.message(player, "Combat", "You cannot place a cannon in this world.");
            return false;
        }

        Cannon cannon = this.cannonManager.spawn(player.getUniqueId(), cannonLocation);
        if (cannon != null) {
            UtilMessage.message(player, "Combat", "You placed a <alt2>Cannon</alt2>!");
            return true;
        }
        return false;
    }
    
    private boolean canUse(Player player) {
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
} 