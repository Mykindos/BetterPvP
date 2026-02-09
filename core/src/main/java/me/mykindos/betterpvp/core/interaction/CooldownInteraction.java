package me.mykindos.betterpvp.core.interaction;

import lombok.Getter;
import me.mykindos.betterpvp.core.cooldowns.Cooldown;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * An interaction that supports cooldowns and energy costs.
 */
@Getter
public abstract class CooldownInteraction extends AbstractInteraction {

    protected final CooldownManager cooldownManager;
    protected final String cooldownName;

    protected CooldownInteraction(@NotNull String name, @NotNull String cooldownName, @NotNull CooldownManager cooldownManager) {
        super(name);
        this.cooldownName = cooldownName;
        this.cooldownManager = cooldownManager;
    }

    protected CooldownInteraction(@NotNull String name, @NotNull CooldownManager cooldownManager) {
        this(name, UtilFormat.cleanString(name), cooldownManager);
    }

    /**
     * Get the cooldown duration in seconds.
     *
     * @return the cooldown in seconds
     */
    public abstract double getCooldown();

    /**
     * Get the energy cost for this interaction.
     * Override to specify an energy cost.
     *
     * @return the energy cost (default 0)
     */
    public double getEnergyCost() {
        return 0;
    }

    /**
     * Whether to inform the player about cooldown status.
     *
     * @return true to show cooldown messages (default true)
     */
    public boolean informCooldown() {
        return true;
    }

    /**
     * Whether to inform the player about insufficient energy.
     *
     * @return true to show energy messages (default true)
     */
    public boolean informEnergy() {
        return true;
    }

    /**
     * Whether to show cooldown on action bar.
     *
     * @return true to show action bar cooldown (default false)
     */
    public boolean showActionBarCooldown() {
        return false;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // Check energy cost
        double energyCost = getEnergyCost();
        if (energyCost > 0) {
            if (!actor.hasEnergy(energyCost)) {
                return new InteractionResult.Fail(InteractionResult.FailReason.ENERGY);
            }
        }

        // Check cooldown (only for players)
        double cooldown = getCooldown();
        if (cooldown > 0 && actor.isPlayer()) {
            Player player = (Player) actor.getEntity();
            if (cooldownManager.hasCooldown(player, this.cooldownName)) {
                final Cooldown cd = cooldownManager.getObject(player.getUniqueId()).orElseThrow().get(this.cooldownName);
                if (cd.isInform()) cooldownManager.informCooldown(player, this.cooldownName);
                return new InteractionResult.Fail(InteractionResult.FailReason.COOLDOWN);
            }
        }

        // Execute the actual interaction
        return doCooldownExecute(actor, context, itemInstance, itemStack);
    }

    @Override
    public void then(@NotNull InteractionActor actor, @NotNull InteractionContext context, @NotNull InteractionResult result, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        super.then(actor, context, result, itemInstance, itemStack);
        if (result.isSuccess() && actor.isPlayer()) {
            Player player = (Player) actor.getEntity();

            final double cooldown = getCooldown();
            if (cooldown > 0) {
                cooldownManager.use(player, this.cooldownName, cooldown, informCooldown(), true, false, showActionBarCooldown());
            }

            // Use energy after cooldown check passes
            final double energyCost = getEnergyCost();
            if (energyCost > 0) {
                actor.useEnergy(this.cooldownName, energyCost, informEnergy());
            }
        }
    }

    /**
     * Perform the actual interaction execution after cooldown and energy checks pass.
     *
     * @param actor        the actor performing the interaction
     * @param context      the interaction context
     * @param itemInstance the item instance (may be null)
     * @param itemStack    the item stack (may be null)
     * @return the result of the interaction
     */
    @NotNull
    protected abstract InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack);
}
