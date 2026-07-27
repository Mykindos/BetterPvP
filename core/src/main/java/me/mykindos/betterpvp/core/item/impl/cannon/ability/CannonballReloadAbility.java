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
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmo;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmoRegistry;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonState;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Chambers whatever round the held item represents. The ability no longer knows about any particular cannonball -
 * it asks {@link CannonAmmoRegistry} what the item loads, so a new ammo type needs no change here.
 */
@Getter
@Setter
@Singleton
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CannonballReloadAbility extends AbstractInteraction implements DisplayedInteraction {

    private final CannonService cannonService;
    private final CannonAmmoRegistry ammoRegistry;

    @Inject
    public CannonballReloadAbility(Core core, CannonService cannonService, CannonAmmoRegistry ammoRegistry) {
        super("cannonball_reload");
        this.cannonService = cannonService;
        this.ammoRegistry = ammoRegistry;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("core.ability.cannonball-reload.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("core.ability.cannonball-reload.description");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                   @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player) || !canUse(player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final CannonAmmo ammo = ammoRegistry.byItem(itemInstance).orElse(null);
        if (ammo == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final RayTraceResult trace = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getEyeLocation().getDirection(), 3, 0.1, entity -> !entity.equals(player));
        if (trace == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Entity targetEntity = trace.getHitEntity();
        if (targetEntity == null || !player.hasLineOfSight(trace.getHitPosition().toLocation(player.getWorld()))) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Optional<CannonProp> cannonOpt = cannonService.of(targetEntity);
        if (cannonOpt.isEmpty()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final CannonProp cannon = cannonOpt.get();
        if (cannon.getCycleState() != CannonState.IDLE || !cannon.getArchetype().getFiringMode().consumesAmmo()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (!cannon.getArchetype().accepts(ammo.id())) {
            UtilMessage.message(player, "core.prefix.combat", "core.cannon.wrong_ammo", ammo.displayName().color(NamedTextColor.YELLOW));
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final CannonReloadEvent reloadEvent = new CannonReloadEvent(cannon, player, ammo);
        reloadEvent.callEvent();
        if (!reloadEvent.isCancelled()) {
            return InteractionResult.Success.ADVANCE;
        }
        return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
    }

    private boolean canUse(Player player) {
        if (!Compatibility.MODEL_ENGINE) {
            UtilMessage.message(player, "core.prefix.combat", "core.cannon.not_supported",
                    Translations.component("core.cannon.contact_admin").color(NamedTextColor.RED));
            return false;
        }
        return true;
    }
}
