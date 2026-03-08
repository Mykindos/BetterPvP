package me.mykindos.betterpvp.core.item.impl.cannon.ability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Getter
@Setter
@Singleton
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CannonballReloadAbility extends AbstractInteraction implements DisplayedInteraction {

    private final CannonManager cannonManager;

    @Inject
    public CannonballReloadAbility(Core core, CannonManager cannonManager) {
        super("cannonball_reload");
        this.cannonManager = cannonManager;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Cannonball Reload");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Load a cannon with this cannonball");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (!canUse(player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final RayTraceResult trace = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                3,
                0.1,
                entity -> !entity.equals(player));

        if (trace == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Entity targetEntity = trace.getHitEntity();
        if (targetEntity == null || !player.hasLineOfSight(trace.getHitPosition().toLocation(player.getWorld()))) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Optional<Cannon> cannonOpt = this.cannonManager.of(targetEntity);
        if (cannonOpt.isEmpty()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Cannon cannon = cannonOpt.get();
        if (cannon.isLoaded()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final CannonReloadEvent cannonReloadEvent = new CannonReloadEvent(cannon, player);
        cannonReloadEvent.callEvent();
        if (!cannonReloadEvent.isCancelled()) {
            return InteractionResult.Success.ADVANCE;
        }
        return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
    }

    private boolean canUse(Player player) {
        if (!Compatibility.MODEL_ENGINE) {
            UtilMessage.message(player, "Combat", "Cannons are not supported on this server. <red>Please contact an administrator.");
            return false;
        }
        return true;
    }
} 