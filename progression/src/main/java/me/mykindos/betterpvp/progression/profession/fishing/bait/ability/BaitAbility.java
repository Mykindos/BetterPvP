package me.mykindos.betterpvp.progression.profession.fishing.bait.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base ability for all bait types.
 * Handles common functionality like throwing bait and applying effects.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class BaitAbility extends CooldownInteraction implements DisplayedInteraction {

    protected double radius;
    protected double multiplier;
    protected long duration;

    /**
     * Creates a new bait ability
     *
     * @param name The name of the ability
     * @param cooldownManager The cooldown manager
     */
    protected BaitAbility(String name, CooldownManager cooldownManager) {
        super(name, cooldownManager);
    }

    @Override
    public double getCooldown() {
        return 5.0;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Remove the item from hand
        if (itemStack != null) {
            UtilInventory.remove(player, itemStack.getType(), 1);
        }
        player.swingMainHand();

        // Create and throw the bait
        UtilServer.callEvent(new PlayerThrowBaitEvent(player, createBait()));

        // Send message
        final TextComponent name = Component.text(getName()).color(NamedTextColor.YELLOW);
        UtilMessage.message(player, "Bait", Component.text("You used ", NamedTextColor.GRAY).append(name));
        return InteractionResult.Success.ADVANCE;
    }
    
    /**
     * Creates a new bait instance with the current settings
     *
     * @return The created bait
     */
    protected abstract Bait createBait();
} 