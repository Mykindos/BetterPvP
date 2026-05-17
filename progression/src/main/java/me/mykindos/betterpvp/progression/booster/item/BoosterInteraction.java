package me.mykindos.betterpvp.progression.booster.item;

import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.actor.PlayerInteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.booster.BoosterManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BoosterInteraction extends AbstractInteraction implements DisplayedInteraction {

    private final BoosterManager boosterManager;
    private final long durationMillis;

    public BoosterInteraction(BoosterManager boosterManager, long durationMillis) {
        super("Booster");
        this.boosterManager = boosterManager;
        this.durationMillis = durationMillis;
        this.setConsumesItem(true);
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor instanceof PlayerInteractionActor playerActor)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CANCELLED);
        }
        
        Player player = playerActor.getPlayer();
        UUID uuid = player.getUniqueId();

        boosterManager.activateBooster(uuid, durationMillis);

        UtilMessage.simpleMessage(player, "Booster", Component.text("You have activated a ", NamedTextColor.GRAY)
                .append(Component.text("Profession Experience Booster", NamedTextColor.GREEN))
                .append(Component.text("!", NamedTextColor.GRAY)));
        
        return InteractionResult.Success.ADVANCE;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Activate Booster", NamedTextColor.GREEN);
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Activates a 20% Profession Experience Booster for 24 hours.", NamedTextColor.GRAY);
    }
}
