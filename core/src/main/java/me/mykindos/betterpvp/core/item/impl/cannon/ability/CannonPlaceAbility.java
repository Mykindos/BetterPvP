package me.mykindos.betterpvp.core.item.impl.cannon.ability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.impl.cannon.event.PreCannonPlaceEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Singleton
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CannonPlaceAbility extends CooldownInteraction implements DisplayedInteraction {

    @EqualsAndHashCode.Include
    private double cooldown;
    private final CannonManager cannonManager;
    private final Map<UUID, Location> cannonLocations = new HashMap<>();

    @Inject
    private CannonPlaceAbility(Core core, CannonManager cannonManager, CooldownManager cooldownManager) {
        super("cannon_placement", cooldownManager);
        this.cannonManager = cannonManager;
        this.cooldown = cannonManager.getSpawnCooldown();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Cannon Placement");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Place a cannon that can be loaded with cannonballs");
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (!canUse(player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Location cannonLocation = cannonLocations.remove(player.getUniqueId());

        if(!cannonLocation.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {
            UtilMessage.message(player, "Combat", "You cannot place a cannon in this world.");
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        Cannon cannon = this.cannonManager.spawn(player.getUniqueId(), cannonLocation);
        if (cannon != null) {
            UtilMessage.message(player, "Combat", "You placed a <alt2>Cannon</alt2>!");
            return InteractionResult.Success.ADVANCE;
        }
        return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
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