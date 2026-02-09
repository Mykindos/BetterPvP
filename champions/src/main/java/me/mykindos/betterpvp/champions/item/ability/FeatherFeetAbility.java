package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A reusable ability that prevents fall damage when the player is holding an item with this ability.
 */
@EqualsAndHashCode(callSuper = false)
public class FeatherFeetAbility extends AbstractInteraction implements DisplayedInteraction, Listener {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;

    public FeatherFeetAbility(ItemFactory itemFactory) {
        super("feather_feet");
        this.itemFactory = itemFactory;
        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(Champions.class));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Feather Feet");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Prevents all incoming damage from falling while holding this item.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // This is a passive ability, so no active invocation
        return InteractionResult.Success.NO_ADVANCE;
    }

    /**
     * Cancels fall damage when a player is holding an item with this ability
     */
    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        componentCancel(event, mainHand);

        // Check off hand (optional functionality)
        ItemStack offHand = player.getInventory().getItemInOffHand();
        componentCancel(event, offHand);
    }

    private void componentCancel(EntityDamageEvent event, ItemStack mainHand) {
        if (mainHand.getType() != Material.AIR) {
            itemFactory.fromItemStack(mainHand).ifPresent(instance -> {
                Optional<InteractionContainerComponent> container = instance.getComponent(InteractionContainerComponent.class);
                if (container.isPresent() && container.get().getChain().hasRoot(this)) {
                    event.setCancelled(true);
                }
            });
        }
    }
}
