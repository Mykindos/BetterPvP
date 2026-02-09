package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EnergyBoost extends CooldownInteraction implements DisplayedInteraction {

    @EqualsAndHashCode.Include
    private double energy;
    @EqualsAndHashCode.Include
    private double cooldown;

    private final EnergyService energyService;
    private final SoundEffect soundEffect;

    public EnergyBoost(EnergyService energyService, CooldownManager cooldownManager, SoundEffect soundEffect) {
        super("energy_boost", cooldownManager);
        this.energyService = energyService;
        this.soundEffect = soundEffect;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Energy Boost");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Instantly grants a flat energy boost when used.");
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        energyService.regenerateEnergy(player, energy, EnergyEvent.Cause.USE);
        UtilMessage.message(player, "Item", Component.text("You used ", NamedTextColor.GRAY)
                .append(getDisplayName().applyFallbackStyle(NamedTextColor.YELLOW))
                .append(Component.text(".", NamedTextColor.GRAY)));
        soundEffect.play(player);
        return InteractionResult.Success.ADVANCE;
    }
}
