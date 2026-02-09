package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ReverseKnockbackAbility extends AbstractInteraction implements DisplayedInteraction, Listener {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;
    @EqualsAndHashCode.Exclude
    private final BaseItem heldItem;
    private double knockbackMultiplier;
    private boolean bypassMinimum;

    public ReverseKnockbackAbility(Champions champions, ItemFactory itemFactory, BaseItem heldItem) {
        super("magnetic_pull");
        this.itemFactory = itemFactory;
        this.heldItem = heldItem;
        this.knockbackMultiplier = -1.0; // Default is to reverse knockback
        this.bypassMinimum = true;       // Default is to bypass minimum knockback
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Magnetic Pull");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Enemies hit by this weapon will be pulled inwards instead of pushed away.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // This ability is passive and doesn't need direct invocation
        return InteractionResult.Success.ADVANCE;
    }

    /**
     * Handle custom knockback for weapons with this ability
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKnockback(CustomKnockbackEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        DamageCause cause = event.getDamageEvent().getCause();
        if (!cause.getCategories().contains(DamageCauseCategory.MELEE)) {
            return; // Only apply to melee attacks
        }

        // Check if the weapon is being held
        itemFactory.fromItemStack(damager.getInventory().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != heldItem) return; // Ensure the held item matches

            // Apply custom knockback settings
            event.setCanBypassMinimum(bypassMinimum);
            event.setMultiplier(event.getMultiplier() * knockbackMultiplier);
        });
    }
}
