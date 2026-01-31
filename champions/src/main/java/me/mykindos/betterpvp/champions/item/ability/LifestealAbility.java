package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LifestealAbility extends AbstractInteraction implements Listener {

    @EqualsAndHashCode.Exclude
    private final BaseItem baseItem;
    private final Function<Player, Double> healFunction;
    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;

    public LifestealAbility(Champions champions, ItemFactory itemFactory, BaseItem baseItem, Function<Player, Double> healFunction) {
        super("Lifesteal",
                "Heal for a portion of the damage dealt to an enemy from melee attacks.");
        this.itemFactory = itemFactory;
        this.baseItem = baseItem;
        this.healFunction = healFunction;
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // This is a passive ability, healing is handled in the event listener below
        return InteractionResult.Success.ADVANCE;
    }

    /**
     * Apply healing when the player damages an entity with the Scythe
     */
    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        itemFactory.fromItemStack(damager.getEquipment().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != baseItem) return; // Ensure the held item matches

            UtilPlayer.health(damager, healFunction.apply(damager));
        });
    }
}
