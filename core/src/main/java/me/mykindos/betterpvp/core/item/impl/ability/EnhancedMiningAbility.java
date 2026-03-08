package me.mykindos.betterpvp.core.item.impl.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides enhanced mining speed to the Runed Pickaxe.
 * This ability is passive and does not need to be triggered.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnhancedMiningAbility extends AbstractInteraction implements DisplayedInteraction {

    private double miningSpeed;

    public EnhancedMiningAbility() {
        super("enhanced_mining");
        this.miningSpeed = 30.0; // Default value, will be overridden by config
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Enhanced Mining");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Grants enhanced mining speed for stone-based blocks. Works exactly like a pickaxe.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // This is a passive ability and doesn't need active invocation
        return InteractionResult.Success.ADVANCE;
    }
    
    /**
     * Applies the mining speed enhancement to the item.
     * Called during item initialization.
     *
     * @param itemStack The item to modify
     */
    public void applyMiningSpeed(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            ToolComponent toolComponent = meta.getTool();
            toolComponent.addRule(Tag.MINEABLE_PICKAXE, (float) miningSpeed, true);
            meta.setTool(toolComponent);
            itemStack.setItemMeta(meta);
        }
    }
} 