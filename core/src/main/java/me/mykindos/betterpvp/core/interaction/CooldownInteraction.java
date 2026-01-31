package me.mykindos.betterpvp.core.interaction;

import lombok.Getter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
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

    protected CooldownInteraction(@NotNull String name, @NotNull String description,
                                  @NotNull CooldownManager cooldownManager) {
        super(name, description);
        this.cooldownManager = cooldownManager;
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
            if (!cooldownManager.use(player, getName(), cooldown, informCooldown(), true, false, showActionBarCooldown())) {
                return new InteractionResult.Fail(InteractionResult.FailReason.COOLDOWN);
            }
        }

        // Use energy after cooldown check passes
        if (energyCost > 0) {
            if (!actor.useEnergy(getName(), energyCost, informEnergy())) {
                // Rollback cooldown if energy use fails
                if (cooldown > 0 && actor.isPlayer()) {
                    Player player = (Player) actor.getEntity();
                    cooldownManager.removeCooldown(player, getName(), true);
                }
                return new InteractionResult.Fail(InteractionResult.FailReason.ENERGY);
            }
        }

        // Execute the actual interaction
        return doCooldownExecute(actor, context, itemInstance, itemStack);
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
