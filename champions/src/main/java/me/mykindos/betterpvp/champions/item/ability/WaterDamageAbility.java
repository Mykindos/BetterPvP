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
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageModifier;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
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
public class WaterDamageAbility extends AbstractInteraction implements Listener {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;
    @EqualsAndHashCode.Exclude
    private final BaseItem heldItem;
    private double bonusDamage;

    public WaterDamageAbility(Champions champions, ItemFactory itemFactory, BaseItem heldItem) {
        super("Water Damage", "Increases damage dealt by melee attacks while in water by a flat amount.");
        this.itemFactory = itemFactory;
        this.heldItem = heldItem;
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // This is a passive ability
        return InteractionResult.Success.ADVANCE;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        // Add bonus damage if in water
        if (!damager.getLocation().getBlock().isLiquid()) {
            return;
        }

        itemFactory.fromItemStack(damager.getEquipment().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != heldItem) return; // Ensure the held item matches

            event.addModifier(new InteractionDamageModifier.Flat(this, bonusDamage));
        });
    }
}
